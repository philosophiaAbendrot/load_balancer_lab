package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.ConfigImpl;
import loadbalancerlab.shared.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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

public class ClientRequestHandlerServerIntegrationTest {
    ClientRequestHandler mockClientReqHandler;
    ClientRequestHandlerServer clientRequestHandlerServer;
    CacheRedistributor mockCacheRedis;
    Thread clientRequestHandlerServerThread;
    int selectedPort = 1;
    int clientRequestHandlerServerPort;
    CloseableHttpClient httpClient;
    String resourceName;
    HttpClientFactory mockClientFactory;
    CloseableHttpClient mockClient;
    CloseableHttpResponse mockResponse;
    String mockEntityContent = "resource_content.jpg";
    Config config;

    @BeforeAll
    public static void config() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.THREAD_MANAGEMENT });
    }

    @BeforeEach
    public void setup() throws IOException {
        // setting up mocks
        mockCacheRedis = Mockito.mock(CacheRedistributor.class);
        when(mockCacheRedis.selectPort(anyString())).thenReturn(selectedPort);

        mockClientReqHandler = new ClientRequestHandler(mockCacheRedis);
        clientRequestHandlerServer = new ClientRequestHandlerServer(mockClientReqHandler);
        clientRequestHandlerServerThread = new Thread(clientRequestHandlerServer);
        clientRequestHandlerServerThread.start();

        mockClient = Mockito.mock(CloseableHttpClient.class);
        mockResponse = Mockito.mock(CloseableHttpResponse.class);
        StatusLine mockResponseStatus = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        when(mockResponse.getStatusLine()).thenReturn(mockResponseStatus);
        HttpEntity mockEntity = Mockito.mock(HttpEntity.class);
        InputStream contentStream = IOUtils.toInputStream(mockEntityContent, StandardCharsets.UTF_8.name());
        when(mockEntity.getContent()).thenReturn(contentStream);
        contentStream.close();

        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        mockClientFactory = Mockito.mock(HttpClientFactory.class);
        when(mockClientFactory.buildApacheClient()).thenReturn(mockClient);

        // passing config to ClientRequestHandler
        config = new ConfigImpl();
        config.setClientFactory(mockClientFactory);
        ClientRequestHandler.configure(config);

        while (clientRequestHandlerServer.getPort() == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }

        clientRequestHandlerServerPort = clientRequestHandlerServer.getPort();
        httpClient = HttpClients.createDefault();
    }

    @AfterEach
    public void shutdown() {
        clientRequestHandlerServerThread.interrupt();
    }

    @Test
    @DisplayName("Sending request to client request handler should call selectPort on cacheRedis with the provided resource name")
    public void shouldCallCacheRedisSelectPort() throws IOException {
        resourceName = "Chooder_Bunny.jpg";
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + clientRequestHandlerServerPort + "/resource/" + resourceName);
        httpClient.execute(getReq);
        ArgumentCaptor<String> resourceNameArg = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockCacheRedis, times(1)).selectPort(resourceNameArg.capture());
        assertEquals(resourceName, resourceNameArg.getValue());
    }
}