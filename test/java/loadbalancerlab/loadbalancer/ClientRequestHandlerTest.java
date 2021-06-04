package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.ConfigImpl;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// tests client request handler and client request handler server together
public class ClientRequestHandlerTest {
    CacheRedistributor cacheRedis;
    ClientRequestHandler reqHandler;
    static int defaultPort = 3_000;
    Thread mockServerThread;
    MockServer mockServerRunnable;
    Config config;
    HttpClientFactory clientFactory;
    int mockServerPort;
    CloseableHttpClient mockClient;
    CloseableHttpResponse mockResponse;
    int mockCacheServerPort = 5_846;

    private class MockServer implements Runnable {
        ClientRequestHandler reqHandler;
        CacheRedistributor cacheRedis;
        public int port = -1;

        public MockServer(ClientRequestHandler _reqHandler, CacheRedistributor _cacheRedis) {
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
                        .registerHandler("/api/*", reqHandler)
                        .create();

                try {
                    server.start();
                } catch (IOException e) {
                    System.out.println("LoadBalancer | Failed to start server on port " + temporaryPort);
                    temporaryPort++;
                    continue;
                }

                int port = temporaryPort;
                break;
            }

            HttpServer finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    finalServer.shutdown(1, TimeUnit.SECONDS);
                }
            });
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        // setup configuration
        clientFactory = new HttpClientFactoryImpl();
        config = new ConfigImpl();
        mockClient = Mockito.mock(CloseableHttpClient.class);
        mockResponse = Mockito.mock(CloseableHttpResponse.class);
        when(mockClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        HttpClientFactory mockClientFactory = Mockito.mock(HttpClientFactory.class);
        when(mockClientFactory.buildApacheClient()).thenReturn(mockClient);
        config.setClientFactory(mockClientFactory);
        ClientRequestHandler.configure(config);

        cacheRedis = Mockito.mock(CacheRedistributorImpl.class);
        when(cacheRedis.selectPort(anyString())).thenReturn(mockCacheServerPort);

        reqHandler = new ClientRequestHandler(cacheRedis);
        mockServerRunnable = new MockServer(reqHandler, cacheRedis);
        mockServerThread = new Thread(mockServerRunnable);
        mockServerThread.start();


        while (true) {
            if (mockServerRunnable.port != -1) {
                mockServerPort = mockServerRunnable.port;
                break;
            }
        }
    }

    @Test
    @DisplayName("Sending a request to it should cause findServer to be called on cacheRedistributorImpl")
    public void sendingRequestShouldCauseFindServerToBeCalled() throws IOException {
        CloseableHttpClient client = clientFactory.buildApacheClient();
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + mockServerPort + "/api/cache-servers");
        CloseableHttpResponse res = client.execute(getReq);
        ArgumentCaptor<HttpGet> argCaptor = ArgumentCaptor.forClass(HttpGet.class);
        verify(mockClient, times(1)).execute(argCaptor.capture());
        URI reqUri = argCaptor.getValue().getURI();
        assertEquals("http://127.0.0.1:" + mockCacheServerPort + "/", reqUri);
    }

    @Test
    @DisplayName("The request should be forwarded to the selected server port")
    public void requestShouldBeForwardedToSelectedServerPort() {

    }

    @AfterEach
    public void shutdown() {
        mockServerThread.interrupt();
    }
}