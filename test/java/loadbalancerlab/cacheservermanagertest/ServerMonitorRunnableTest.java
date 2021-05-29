package loadbalancerlab.cacheservermanagertest;

import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.cacheservermanager.CacheServerManager;
import loadbalancerlab.cacheservermanager.ServerMonitor;
import loadbalancerlab.cacheservermanager.ServerMonitorRunnable;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.services.monitor.CapacityFactorMonitor;
import loadbalancerlab.services.monitor.CapacityFactorMonitorImpl;
import loadbalancerlab.util.Logger;
import loadbalancerlab.util.RequestDecoder;
import loadbalancerlab.util.RequestDecoderImpl;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class ServerMonitorRunnableTest {
    HttpClientFactory clientFactory;
    CloseableHttpClient mockClient;
    long currentTime;

    @BeforeEach
    public void setup() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.CAPACITY_MODULATION });
        this.clientFactory = Mockito.mock(HttpClientFactory.class);
        this.mockClient = Mockito.mock(CloseableHttpClient.class);
        this.currentTime = System.currentTimeMillis();
    }

    @Nested
    @DisplayName("Testing server analytics")
    public class TestServerAnalytics {
        @Test
        @DisplayName("Should return the correct number of active servers by second")
        public void returnActiveServers() {

        }

        @Test
        @DisplayName("If there are duplicate entries for a given second, the first one should be recorded")
        public void ignoreDuplicateInputs() {

        }
    }

    @Nested
    @DisplayName("Tests pingCacheServers() method")
    class TestPingCacheServers {
        ArgumentCaptor<HttpUriRequest> argument;
        int cacheServerPort;
        ServerMonitorRunnable serverMonitorRunnable;
        JSONObject mockJsonResponse;
        RequestDecoder mockDecoder;
        CloseableHttpResponse mockResponse;
        CacheServerManager mockCacheServerManager;
        int numServers = 3;

        @BeforeEach
        public void setup() throws IOException {
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.mockCacheServerManager = Mockito.mock(CacheServerManager.class);
            this.argument = ArgumentCaptor.forClass(HttpGet.class);

            // set up canned response regarding the port of the server that the cache server was started up on which is returned when a request is made through the
            // mock HttpClient to the (virtual) CacheServerManager to start a server
            this.cacheServerPort = 1_000;
            this.mockJsonResponse = new JSONObject();
            this.mockJsonResponse.put("port", this.cacheServerPort);
            this.mockJsonResponse.put("capacity_factor", 0.5);
            when(ServerMonitorRunnableTest.this.clientFactory.buildApacheClient()).thenReturn(ServerMonitorRunnableTest.this.mockClient);
            this.mockResponse = Mockito.mock(CloseableHttpResponse.class);
            when(ServerMonitorRunnableTest.this.mockClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockResponse);
            when(this.mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);

            this.serverMonitorRunnable = new ServerMonitorRunnable(clientFactory, mockDecoder, mockCacheServerManager);

            // add server info instances to monitor
            for (int i = 0; i < numServers; i++)
                this.serverMonitorRunnable.addServer(i, 10_000 + i);

            this.serverMonitorRunnable.pingCacheServers(System.currentTimeMillis() / 1_000);
        }

        @Test
        @DisplayName("Test that request is sent to CacheServer for update on capacity factor")
        public void shouldSendRequestWithPostMethod() throws IOException {
            verify(ServerMonitorRunnableTest.this.mockClient, times(numServers)).execute(any(HttpGet.class));
        }

        @Test
        @DisplayName("Request should be of type POST")
        public void shouldSendRequestToCacheServerManager() throws IOException {
            verify(ServerMonitorRunnableTest.this.mockClient, times(numServers)).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(1);
            assertEquals("GET", request.getMethod());
        }

        @Test
        @DisplayName("Request should go to correct URI")
        public void requestShouldGoToCorrectURI() throws IOException {
            verify(ServerMonitorRunnableTest.this.mockClient, times(numServers)).execute(this.argument.capture());
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