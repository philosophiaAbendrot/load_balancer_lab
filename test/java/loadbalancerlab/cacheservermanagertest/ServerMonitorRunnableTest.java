package loadbalancerlab.cacheservermanagertest;

import loadbalancerlab.cacheservermanager.CacheServerManager;
import loadbalancerlab.cacheservermanager.ServerMonitorRunnable;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.util.Logger;
import loadbalancerlab.util.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ServerMonitorRunnableTest {
    HttpClientFactory clientFactory;
    CloseableHttpClient mockClient;
    long currentTime;
    ServerMonitorRunnable serverMonitorRunnable;
    CacheServerManager mockCacheServerManager;
    RequestDecoder mockDecoder;

    @BeforeEach
    public void setup() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.CAPACITY_MODULATION });
        this.clientFactory = Mockito.mock(HttpClientFactory.class);
        this.mockClient = Mockito.mock(CloseableHttpClient.class);
        this.currentTime = System.currentTimeMillis();
        this.mockDecoder = Mockito.mock(RequestDecoder.class);
        this.mockCacheServerManager = Mockito.mock(CacheServerManager.class);
    }

    @Nested
    @DisplayName("Testing deliverData()")
    public class TestDeliverData {
        @BeforeEach
        public void setup() {
            serverMonitorRunnable = new ServerMonitorRunnable(clientFactory, mockDecoder, ServerMonitorRunnableTest.this.mockCacheServerManager);
        }

        @Test
        @DisplayName("Should return the correct number of active servers by second")
        public void returnActiveServers() {
            int currentTime = (int)System.currentTimeMillis() / 1_000;
            serverMonitorRunnable.updateServerCount(currentTime + 1, 10);
            serverMonitorRunnable.updateServerCount(currentTime + 2, 12);
            serverMonitorRunnable.updateServerCount(currentTime + 3, 15);
            SortedMap<Integer, Integer> serverData = serverMonitorRunnable.deliverData();

            assertTrue(serverData.containsKey(currentTime + 1));
            assertTrue(serverData.containsKey(currentTime + 2));
            assertTrue(serverData.containsKey(currentTime + 3));
            assertEquals(serverData.get(currentTime + 1), 10);
            assertEquals(serverData.get(currentTime + 2), 12);
            assertEquals(serverData.get(currentTime + 3), 15);
        }

        @Test
        @DisplayName("If there are duplicate entries for a given second, the first one should be recorded")
        public void ignoreDuplicateInputs() {
            int currentTime = (int)System.currentTimeMillis() / 1_000;
            serverMonitorRunnable.updateServerCount(currentTime + 1, 10);
            serverMonitorRunnable.updateServerCount(currentTime + 2, 12);
            serverMonitorRunnable.updateServerCount(currentTime + 2, 16);
            serverMonitorRunnable.updateServerCount(currentTime + 3, 15);
            SortedMap<Integer, Integer> serverData = serverMonitorRunnable.deliverData();

            assertTrue(serverData.containsKey(currentTime + 2));
            assertEquals(serverData.get(currentTime + 2), 12);
        }
    }

    @Nested
    @DisplayName("Tests pingCacheServers() method")
    class TestPingCacheServers {
        ArgumentCaptor<HttpUriRequest> argument;
        int cacheServerPort;
        JSONObject mockJsonResponse;
        CloseableHttpResponse mockResponse;
        CacheServerManager mockCacheServerManager;
        int numServers = 3;

        @BeforeEach
        public void setup() throws IOException {
            this.mockCacheServerManager = Mockito.mock(CacheServerManager.class);
            this.argument = ArgumentCaptor.forClass(HttpGet.class);

            // set up canned response regarding the port of the server that the cache server was started up on which is returned when a request is made through the
            // mock HttpClient to the (virtual) CacheServerManager to start a server
            this.cacheServerPort = 1_000;
            this.mockJsonResponse = new JSONObject();
            this.mockJsonResponse.put("port", this.cacheServerPort);
            this.mockJsonResponse.put("capacity_factor", 0.5);
            when(clientFactory.buildApacheClient()).thenReturn(mockClient);
            this.mockResponse = Mockito.mock(CloseableHttpResponse.class);
            when(mockClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockResponse);
            when(mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);
            serverMonitorRunnable = new ServerMonitorRunnable(clientFactory, mockDecoder, mockCacheServerManager);

            // add server info instances to monitor
            for (int i = 0; i < numServers; i++)
                serverMonitorRunnable.addServer(i, 10_000 + i);

            serverMonitorRunnable.pingCacheServers(System.currentTimeMillis() / 1_000);
        }

        @Test
        @DisplayName("Test that request is sent to CacheServer for update on capacity factor")
        public void shouldSendRequestWithPostMethod() throws IOException {
            verify(mockClient, times(numServers)).execute(any(HttpGet.class));
        }

        @Test
        @DisplayName("Request should be of type POST")
        public void shouldSendRequestToCacheServerManager() throws IOException {
            verify(mockClient, times(numServers)).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(1);
            assertEquals("GET", request.getMethod());
        }

        @Test
        @DisplayName("Request should go to correct URI")
        public void requestShouldGoToCorrectURI() throws IOException {
            verify(mockClient, times(numServers)).execute(this.argument.capture());
            HttpUriRequest request1 = this.argument.getAllValues().get(0);
            assertEquals("http://127.0.0.1:" + 10000 + "/capacity-factor", request1.getURI().toString());
            HttpUriRequest request2 = this.argument.getAllValues().get(1);
            assertEquals("http://127.0.0.1:" + 10001 + "/capacity-factor", request2.getURI().toString());
            HttpUriRequest request3 = this.argument.getAllValues().get(2);
            assertEquals("http://127.0.0.1:" + 10002 + "/capacity-factor", request3.getURI().toString());
        }
    }

    @Nested
    @DisplayName("Tests addServer()")
    class AddServer {

    }

    @Nested
    @DisplayName("Tests updateServerCount()")
    class UpdateServerCount {

    }

    @Nested
    @DisplayName("Tests deliverData()")
    class DeliverData {

    }
}