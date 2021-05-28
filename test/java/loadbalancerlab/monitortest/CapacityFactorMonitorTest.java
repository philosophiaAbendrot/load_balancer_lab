package loadbalancerlab.monitortest;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.services.monitor.CapacityFactorMonitor;
import loadbalancerlab.services.monitor.CapacityFactorMonitorImpl;
import loadbalancerlab.util.Logger;
import loadbalancerlab.util.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class CapacityFactorMonitorTest {
    // test that ping servers sends a request to existing cache servers
    private static final int CACHE_SERVER_MANAGER_PORT = 8080;
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
    @DisplayName("Tests startUpCacheServer() method")
    class TestStartupCacheServer {
        CapacityFactorMonitor capFactorMonitor;
        RequestDecoder mockDecoder;
        JSONObject mockJsonResponse;
        CloseableHttpClient mockClient;
        ArgumentCaptor<HttpUriRequest> argCaptor;
        int hashRingIndex;

        @BeforeEach
        public void setup() {
            this.argCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            this.mockClient = Mockito.mock(CloseableHttpClient.class);
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.CACHE_SERVER_MANAGER_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(this.mockClient);
            this.hashRingIndex = 100;
            this.capFactorMonitor.startupCacheServer(this.hashRingIndex);
        }

        @Test
        @DisplayName("Test that a request is sent to Cache Server Manager")
        public void testRequestSentCacheServerManager() throws IOException {
            verify(this.mockClient, times(1)).execute(any(HttpUriRequest.class));
        }

        @Test
        @DisplayName("Test that a request is sent to CacheServerManager is a POST method")
        public void testRequestSentCacheServerManagerRequestServerStartup() throws IOException {
            verify(this.mockClient, times(1)).execute(this.argCaptor.capture());
            HttpUriRequest request = this.argCaptor.getAllValues().get(0);
            assertEquals("POST", request.getMethod());
        }

        @Test
        @DisplayName("Test that a request is sent to the CacheServerManager is sent to the correct uri")
        public void testRequestSentToCorrectUri() throws IOException {
            verify(this.mockClient, times(1)).execute(this.argCaptor.capture());
            HttpUriRequest request = this.argCaptor.getAllValues().get(0);
            assertEquals("http://127.0.0.1:" + CapacityFactorMonitorTest.CACHE_SERVER_MANAGER_PORT + "/cache-servers", request.getURI().toString());
        }
    }

    @Nested
    @DisplayName("Tests shutdownCacheServer() method")
    class TestShutDownCacheServer {
        CapacityFactorMonitor capFactorMonitor;
        RequestDecoder mockDecoder;
        JSONObject mockJsonResponse;
        CloseableHttpResponse mockResponse;
        CloseableHttpClient mockClient;
        ArgumentCaptor<HttpUriRequest> argCaptor;
        int hashRingIndex;
        int cacheServerPort;

        @BeforeEach
        public void setup() throws IOException {
            this.argCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            this.mockClient = Mockito.mock(CloseableHttpClient.class);
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.CACHE_SERVER_MANAGER_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(this.mockClient);
            this.hashRingIndex = 100;

            // set up canned response regarding the port of the server that the cache server was started up on which is returned when a request is made through the
            // mock HttpClient to the (virtual) CacheServerManager to start a server
            this.cacheServerPort = 1_000;
            this.mockJsonResponse = new JSONObject();
            this.mockJsonResponse.put("port", this.cacheServerPort);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.CACHE_SERVER_MANAGER_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);
            this.mockResponse = Mockito.mock(CloseableHttpResponse.class);
            when(CapacityFactorMonitorTest.this.mockClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockResponse);
            when(this.mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);

            // startup a server
            this.capFactorMonitor.startupCacheServer(this.hashRingIndex);

            // send request to shut down a server
            this.capFactorMonitor.shutdownCacheServer(this.cacheServerPort);
        }

        @Test
        @DisplayName("Test that a request is sent to CacheServerManager")
        public void testRequestSentCacheServerManager() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(1)).execute(any(HttpDelete.class));
        }

        @Test
        @DisplayName("Test that a request is sent to CacheServerManager is a DELETE method")
        public void testRequestSentCacheServerManagerRequestServerStartup() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(2)).execute(this.argCaptor.capture());
            HttpUriRequest request = this.argCaptor.getAllValues().get(1);
            assertEquals("DELETE", request.getMethod());
        }

        @Test
        @DisplayName("Test that a request is sent to the CacheServerManager is sent to the correct uri")
        public void testRequestSentToCorrectUri() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(2)).execute(this.argCaptor.capture());
            HttpUriRequest request = this.argCaptor.getAllValues().get(1);
            assertEquals("http://127.0.0.1:" + CapacityFactorMonitorTest.CACHE_SERVER_MANAGER_PORT + "/cache-server/" + this.cacheServerPort, request.getURI().toString());
        }
    }

    @Nested
    @DisplayName("Tests pingServers() method")
    class TestPingServers {
        CapacityFactorMonitor capFactorMonitor;
        int hashRingIndex;
        ArgumentCaptor<HttpUriRequest> argument;
        int cacheServerPort;
        JSONObject mockJsonResponse;
        RequestDecoder mockDecoder;
        CloseableHttpResponse mockResponse;

        @BeforeEach
        public void setup() throws IOException {
            this.hashRingIndex = 1_000;
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.argument = ArgumentCaptor.forClass(HttpGet.class);

            // set up canned response regarding the port of the server that the cache server was started up on which is returned when a request is made through the
            // mock HttpClient to the (virtual) CacheServerManager to start a server
            this.cacheServerPort = 1_000;
            this.mockJsonResponse = new JSONObject();
            this.mockJsonResponse.put("port", this.cacheServerPort);
            this.mockJsonResponse.put("capacity_factor", 0.5);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.CACHE_SERVER_MANAGER_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);
            this.mockResponse = Mockito.mock(CloseableHttpResponse.class);
            when(CapacityFactorMonitorTest.this.mockClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockResponse);
            when(this.mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);

            // startup a server
            this.capFactorMonitor.startupCacheServer(this.hashRingIndex);

            // run pingServer()
            try {
                this.capFactorMonitor.pingServers(System.currentTimeMillis());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Test
        @DisplayName("Test that request is sent to CacheServerManager for update on capacity factor")
        public void shouldSendRequestWithPostMethod() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(1)).execute(any(HttpGet.class));
        }

        @Test
        @DisplayName("Request should be of type POST")
        public void shouldSendRequestToCacheServerManager() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(2)).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(1);
            assertEquals("GET", request.getMethod());
        }

        @Test
        @DisplayName("Request should go to correct URI")
        public void requestShouldGoToCorrectURI() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(2)).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(1);
            assertEquals("http://127.0.0.1:" + this.cacheServerPort + "/capacity_factor", request.getURI().toString());
        }
    }

    @Nested
    @DisplayName("Tests capacity modulation logic on pingServers()")
    class CapacityModulationLogicOnPingServers {
        CapacityFactorMonitor capFactorMonitor;
        CapacityFactorMonitor capFactorMonitorSpy;
        int hashRingIndex;
        ArgumentCaptor<HttpUriRequest> argument;
        int cacheServerPort;
        JSONObject mockJsonResponse;
        RequestDecoder mockDecoder;
        CloseableHttpResponse mockResponse;

        @BeforeEach
        public void setup() throws IOException {
            this.hashRingIndex = 1_000;
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.argument = ArgumentCaptor.forClass(HttpGet.class);

            // set up canned response regarding the port of the server that the CacheServer was started up on which is returned when a request is made through the
            // mock HttpClient to the (virtual) CacheServerManager to start a server
            this.cacheServerPort = 1_000;
            this.mockJsonResponse = new JSONObject();
            this.mockJsonResponse.put("port", this.cacheServerPort);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.CACHE_SERVER_MANAGER_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);
            this.mockResponse = Mockito.mock(CloseableHttpResponse.class);
            when(CapacityFactorMonitorTest.this.mockClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockResponse);
            when(this.mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);

            // startup a server
            this.capFactorMonitor.startupCacheServer(this.hashRingIndex);
            // add a spy to the server
            this.capFactorMonitorSpy = Mockito.spy(this.capFactorMonitor);
        }

        @Test
        @DisplayName("When capacity factor is higher than threshold, call cacheServerStartup() method")
        public void shouldCallCacheServerStartup() throws IOException {
            this.mockJsonResponse.put("capacity_factor", 0.9);
            this.capFactorMonitorSpy.pingServers(System.currentTimeMillis() + 6_000);
            verify(this.capFactorMonitorSpy, times(1)).startupCacheServer(anyInt());
        }

        @Test
        @DisplayName("When capacity factor is not higher than threshold, startupCacheServer() method is not called")
        public void shouldNotCallStartupCacheServer() throws IOException {
            this.mockJsonResponse.put("capacity_factor", 0.5);
            this.capFactorMonitorSpy.pingServers(System.currentTimeMillis() + 6_000);
            verify(this.capFactorMonitorSpy, times(0)).startupCacheServer(anyInt());
        }

        @Test
        @DisplayName("When capacity factor is lower than threshold, call shutdownCacheServer() method")
        public void shouldCallShutdownCacheServer() throws IOException {
            this.mockJsonResponse.put("capacity_factor", 0.1);
            this.capFactorMonitorSpy.pingServers(System.currentTimeMillis() + 6_000);
            verify(this.capFactorMonitorSpy, times(1)).shutdownCacheServer(anyInt());
        }

        @Test
        @DisplayName("When capacity factor is between the min and max thresholds, shutdownCacheServer() should not be called")
        public void shouldNotCallShutdownCacheServer() throws IOException {
            this.mockJsonResponse.put("capacity_factor", 0.5);
            this.capFactorMonitorSpy.pingServers(System.currentTimeMillis() + 6_000);
            verify(this.capFactorMonitorSpy, times(0)).shutdownCacheServer(anyInt());
        }
    }

    @Nested
    @DisplayName("Tests selectPort() method")
    // test port selection logic based on given resource id
    class TestSelectPort {
        CapacityFactorMonitor capFactorMonitor;
        RequestDecoder mockDecoder;
        int initialCacheServerPort;
        JSONObject mockJsonResponse;

        @BeforeEach
        public void setup() throws IOException {
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.CACHE_SERVER_MANAGER_PORT, this.mockDecoder);
            this.mockJsonResponse = new JSONObject();
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);
            // sets port of first cache server to 3_000
            this.initialCacheServerPort = 3_000;
            this.mockJsonResponse.put("port", this.initialCacheServerPort);
            when(this.mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);
        }

        @Test
        @DisplayName("When the next port is between position 0 and the initial position")
        public void nextPortCounterClockWise() {
            int hashRingIndex = 4_000;
            int resourceId = 105_000;
            int serverPort = this.capFactorMonitor.startupCacheServer(hashRingIndex);
            int selectedPort = this.capFactorMonitor.selectPort(resourceId);
            assertEquals(serverPort, selectedPort);
        }

        @Test
        @DisplayName("When the next port is between the last position and the initial position")
        public void nextPortClockWise() {
            int hashRingIndex = 2_000;
            int resourceId = 105_000;
            int serverPort = this.capFactorMonitor.startupCacheServer(hashRingIndex);
            int selectedPort = this.capFactorMonitor.selectPort(resourceId);
            assertEquals(serverPort, selectedPort);
        }

        @Test
        @DisplayName("When the next port is on the initial position")
        public void nextPortOnInitialPosition() {
            int hashRingIndex = 3_000;
            int resourceId = 105_000;
            int serverPort = this.capFactorMonitor.startupCacheServer(hashRingIndex);
            int selectedPort = this.capFactorMonitor.selectPort(resourceId);
            assertEquals(serverPort, selectedPort);
        }
    }
}