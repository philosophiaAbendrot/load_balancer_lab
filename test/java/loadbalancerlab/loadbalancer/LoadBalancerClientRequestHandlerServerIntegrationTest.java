package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class LoadBalancerClientRequestHandlerServerIntegrationTest {
    static LoadBalancerClientRequestHandler clientReqHandler;
    static ClientRequestHandlerServer clientRequestHandlerServer;
    static CacheRedistributor mockCacheRedis;
    static Thread clientRequestHandlerServerThread;
    static int selectedPort = 1;
    static int clientRequestHandlerServerPort;
    static CloseableHttpClient httpClient;
    static HttpClientFactory mockClientFactory;
    static CloseableHttpClient mockHttpClient;
    static CloseableHttpResponse mockResponse;
    static Config config;
    static String resourceName = "Chooder_Bunny.jpg";
    static String reqPath;
    HttpGet getReq;
    CloseableHttpResponse res;
    static String expectedResponseContent = "resource content";

    @BeforeAll
    public static void config() throws IOException {
        Logger.configure(new Logger.LogType[] { Logger.LogType.THREAD_MANAGEMENT });

        // setting up mocks
        mockCacheRedis = Mockito.mock(CacheRedistributor.class);
        when(mockCacheRedis.selectPort(anyString())).thenReturn(selectedPort);

        // create a mock Http Client and replicate behaviour with mock responses
        mockHttpClient = Mockito.mock(CloseableHttpClient.class);
        mockResponse = Mockito.mock(CloseableHttpResponse.class);
        StatusLine mockResponseStatus = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        when(mockResponse.getStatusLine()).thenReturn(mockResponseStatus);
        HttpEntity resEntity = new StringEntity(expectedResponseContent);
        when(mockResponse.getEntity()).thenReturn(resEntity);
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        mockClientFactory = Mockito.mock(HttpClientFactory.class);
        when(mockClientFactory.buildApacheClient()).thenReturn(mockHttpClient);

        // passing configs to ClientRequestHandler
        config = new Config();
        config.setHttpClientFactory(mockClientFactory);
        LoadBalancerClientRequestHandler.configure(config);
        ClientRequestHandlerServer.configure(config);

        // start up ClientRequestHandler
        clientReqHandler = new LoadBalancerClientRequestHandler(mockCacheRedis);
        clientRequestHandlerServer = new ClientRequestHandlerServer(clientReqHandler);
        clientRequestHandlerServerThread = new Thread(clientRequestHandlerServer);

        // start client request handler server
        clientRequestHandlerServerThread.start();

        while (clientRequestHandlerServer.getPort() == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }

        clientRequestHandlerServerPort = clientRequestHandlerServer.getPort();
        httpClient = HttpClients.createDefault();

        reqPath = "http://127.0.0.1:" + clientRequestHandlerServerPort + "/api/" + resourceName;
    }

    @BeforeEach
    public void setup() throws IOException {
        getReq = new HttpGet(reqPath);
        res = httpClient.execute(getReq);
    }

    @AfterAll
    public static void shutdown() {
        clientRequestHandlerServerThread.interrupt();
    }

    @Test
    @DisplayName("Sending request to client request handler should call selectPort on cacheRedis with the provided resource name")
    public void shouldCallCacheRedisSelectPort() {
        ArgumentCaptor<String> resourceNameArg = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockCacheRedis, times(1)).selectPort(resourceNameArg.capture());
        assertEquals(resourceName, resourceNameArg.getValue());
    }

    @Test
    @DisplayName("Sending request to client request handler should return the response from the cache server")
    public void shouldReturnResponseFromCacheServer() throws IOException {
        HttpEntity resEntity = res.getEntity();
        InputStream contentStream = resEntity.getContent();
        String contentString = IOUtils.toString(contentStream, StandardCharsets.UTF_8);
        assertEquals(expectedResponseContent, contentString);
    }
}