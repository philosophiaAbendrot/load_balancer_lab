package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// tests client request handler and client request handler server together
public class CacheServerLoadBalancerClientRequestHandlerTest {
    static CacheRedistributor cacheRedis;
    static LoadBalancerClientRequestHandler reqHandler;
    static int defaultPort = 3_000;
    static Thread mockServerThread;
    static MockServer mockServerRunnable;
    static Config config;
    static HttpClientFactory clientFactory;
    static int mockServerPort;

    static CloseableHttpClient mockClient;
    static CloseableHttpResponse mockResponse;
    static HttpEntity resEntity;
    static String mockEntityContent = "resource_content.jpg";

    static int mockCacheServerPort = 5_846;

    private static class MockServer implements Runnable {
        LoadBalancerClientRequestHandler reqHandler;
        CacheRedistributor cacheRedis;
        public volatile int port = -1;

        public MockServer( LoadBalancerClientRequestHandler _reqHandler, CacheRedistributor _cacheRedis) {
            reqHandler = _reqHandler;
            cacheRedis = _cacheRedis;
        }

        @Override
        public void run() {
            // start up an apache server to test ClientRequestHandler
            InetAddress hostAddress = null;

            try {
                hostAddress = InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException e) {
                System.out.println("UnknownHostException Within LoadBalancer#run");
                e.printStackTrace();
            }

            SocketConfig config = SocketConfig.custom()
                    .setSoTimeout(15000)
                    .setTcpNoDelay(true)
                    .build();

            HttpServer server;
            int temporaryPort = defaultPort;

            while (true) {
                server = ServerBootstrap.bootstrap()
                        .setLocalAddress(hostAddress)
                        .setListenerPort(temporaryPort)
                        .setHttpProcessor(new ImmutableHttpProcessor(new ArrayList<>(), new ArrayList<>()))
                        .setSocketConfig(config)
                        .registerHandler("/resource/*", reqHandler)
                        .create();

                try {
                    server.start();
                } catch (IOException e) {
                    System.out.println("LoadBalancer | Failed to start server on port " + temporaryPort);
                    temporaryPort++;
                    continue;
                }

                port = temporaryPort;
                break;
            }

            HttpServer finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    finalServer.shutdown(1, TimeUnit.SECONDS);
                }
            });

            try {
                server.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                server.shutdown(1, TimeUnit.SECONDS);
                Thread.currentThread().interrupt();
            }
        }
    }

    @BeforeAll
    public static void setupServer() throws IOException {
        // setup mock CacheRedistributor Impl
        cacheRedis = Mockito.mock(CacheRedistributor.class);
        when(cacheRedis.selectPort(anyString())).thenReturn(mockCacheServerPort);

        // setup configuration
        config = new Config();
        LoadBalancerClientRequestHandler.configure(config);

        // setup and start mock server thread
        reqHandler = new LoadBalancerClientRequestHandler(cacheRedis);
        mockServerRunnable = new MockServer(reqHandler, cacheRedis);
        mockServerThread = new Thread(mockServerRunnable);
        mockServerThread.start();

        clientFactory = new HttpClientFactory();

        while (true) {
            if (mockServerRunnable.port != -1) {
                mockServerPort = mockServerRunnable.port;
                break;
            }
        }
    }

    @AfterAll
    public static void shutdown() {
        mockServerThread.interrupt();
    }

    @BeforeEach
    public void setup() throws IOException {
        // setup mock http client
        mockClient = Mockito.mock(CloseableHttpClient.class);
        HttpClientFactory mockClientFactory = Mockito.mock(HttpClientFactory.class);
        when(mockClientFactory.buildApacheClient()).thenReturn(mockClient);
        reqHandler.clientFactory = mockClientFactory;

        // setting up mocks for mock response
        mockResponse = Mockito.mock(CloseableHttpResponse.class);
        StatusLine mockResponseStatus = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        when(mockResponse.getStatusLine()).thenReturn(mockResponseStatus);
        resEntity = new StringEntity(mockEntityContent);
        when(mockResponse.getEntity()).thenReturn(resEntity);
        when(mockClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
    }

    @Test
    @DisplayName("Sending a request to it should cause findServer to be called on cacheRedistributorImpl")
    public void sendingRequestShouldCauseFindServerToBeCalled() throws IOException {
        CloseableHttpClient client = clientFactory.buildApacheClient();
        String resourceName = "Chooder_Bunny.jpg";
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + mockServerPort + "/resource/" + resourceName);
        CloseableHttpResponse res = client.execute(getReq);
        verify(cacheRedis, times(1)).selectPort(resourceName);
    }

    @Test
    @DisplayName("The request should be forwarded to the selected server port")
    public void requestShouldBeForwardedToSelectedServerPort() throws IOException {
        CloseableHttpClient client = clientFactory.buildApacheClient();
        String resourceName = "Grumpy_Spooky.jpg";
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + mockServerPort + "/resource/" + resourceName);
        client.execute(getReq);
        ArgumentCaptor<HttpGet> argCaptor = ArgumentCaptor.forClass(HttpGet.class);
        verify(mockClient, times(1)).execute(argCaptor.capture());
        URI reqUri = argCaptor.getValue().getURI();
        String expectedPath = "http://127.0.0.1:" + mockCacheServerPort + "/" + resourceName;
        assertEquals(expectedPath, reqUri.toString());
    }

    @Test
    @DisplayName("should forward the response returned by the cache server to the client")
    public void shouldForwardResponseToClient() throws IOException {
        String resourceName = "Grumpy_Spooky.jpg";
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + mockServerPort + "/resource/" + resourceName);
        CloseableHttpClient client = clientFactory.buildApacheClient();
        CloseableHttpResponse res = client.execute(getReq);
        InputStream contentFromMockServer = res.getEntity().getContent();
        String stringFromMockServer = IOUtils.toString(contentFromMockServer, StandardCharsets.UTF_8.name());
        assertEquals(mockEntityContent, stringFromMockServer);
    }
}