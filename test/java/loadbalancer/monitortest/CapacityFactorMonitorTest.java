package loadbalancer.monitortest;

import loadbalancer.factory.ClientFactory;
import loadbalancer.monitor.CapacityFactorMonitor;
import loadbalancer.monitor.CapacityFactorMonitorImpl;
import loadbalancer.util.Logger;
import loadbalancer.util.RequestDecoder;
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
    // test that ping servers sends a request to existing backends
    private static final int BACKEND_INITIATOR_PORT = 8080;
    ClientFactory clientFactory;
    CloseableHttpClient mockClient;
    long currentTime;

    @BeforeEach
    public void setup() {
        Logger.configure(new String[] { "capacityModulation" });
        this.clientFactory = Mockito.mock(ClientFactory.class);
        this.mockClient = Mockito.mock(CloseableHttpClient.class);
        this.currentTime = System.currentTimeMillis();
    }

    @Nested
    @DisplayName("Tests startUpBackEnd() method")
    class TestStartupBackEnd {
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
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.BACKEND_INITIATOR_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(this.mockClient);
            this.hashRingIndex = 100;
            this.capFactorMonitor.startUpBackEnd(this.hashRingIndex);
        }

        @Test
        @DisplayName("Test that a request is sent to BackEndInitiator")
        public void testRequestSentBackEndInitiator() throws IOException {
            verify(this.mockClient, times(1)).execute(any(HttpUriRequest.class));
        }

        @Test
        @DisplayName("Test that a request is sent to BackEndInitiator is a POST method")
        public void testRequestSentBackEndInitiatorRequestServerStartup() throws IOException {
            verify(this.mockClient, times(1)).execute(this.argCaptor.capture());
            HttpUriRequest request = this.argCaptor.getAllValues().get(0);
            assertEquals("POST", request.getMethod());
        }

        @Test
        @DisplayName("Test that a request is sent to the BackEndInitiator is sent to the correct uri")
        public void testRequestSentToCorrectUri() throws IOException {
            verify(this.mockClient, times(1)).execute(this.argCaptor.capture());
            HttpUriRequest request = this.argCaptor.getAllValues().get(0);
            assertEquals("http://127.0.0.1:" + CapacityFactorMonitorTest.BACKEND_INITIATOR_PORT + "/backends", request.getURI().toString());
        }
    }

    @Nested
    @DisplayName("Tests shutDownBackEnd() method")
    class TestShutDownBackEnd {
        CapacityFactorMonitor capFactorMonitor;
        RequestDecoder mockDecoder;
        JSONObject mockJsonResponse;
        CloseableHttpResponse mockResponse;
        CloseableHttpClient mockClient;
        ArgumentCaptor<HttpUriRequest> argCaptor;
        int hashRingIndex;
        int backEndPort;

        @BeforeEach
        public void setup() throws IOException {
            this.argCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            this.mockClient = Mockito.mock(CloseableHttpClient.class);
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.BACKEND_INITIATOR_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(this.mockClient);
            this.hashRingIndex = 100;

            // set up canned response regarding the port of the server that the backend was started up on which is returned when a request is made through the
            // mock HttpClient to the (virtual) BackEndInitiator to start a server
            this.backEndPort = 1_000;
            this.mockJsonResponse = new JSONObject();
            this.mockJsonResponse.put("port", this.backEndPort);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.BACKEND_INITIATOR_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);
            this.mockResponse = Mockito.mock(CloseableHttpResponse.class);
            when(CapacityFactorMonitorTest.this.mockClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockResponse);
            when(this.mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);

            // startup a server
            this.capFactorMonitor.startUpBackEnd(this.hashRingIndex);

            // send request to shut down a server
            this.capFactorMonitor.shutDownBackEnd(this.backEndPort);
        }

        @Test
        @DisplayName("Test that a request is sent to BackEndInitiator")
        public void testRequestSentBackEndInitiator() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(1)).execute(any(HttpDelete.class));
        }

        @Test
        @DisplayName("Test that a request is sent to BackEndInitiator is a DELETE method")
        public void testRequestSentBackEndInitiatorRequestServerStartup() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(2)).execute(this.argCaptor.capture());
            HttpUriRequest request = this.argCaptor.getAllValues().get(1);
            assertEquals("DELETE", request.getMethod());
        }

        @Test
        @DisplayName("Test that a request is sent to the BackEndInitiator is sent to the correct uri")
        public void testRequestSentToCorrectUri() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(2)).execute(this.argCaptor.capture());
            HttpUriRequest request = this.argCaptor.getAllValues().get(1);
            assertEquals("http://127.0.0.1:" + CapacityFactorMonitorTest.BACKEND_INITIATOR_PORT + "/backend/" + this.backEndPort, request.getURI().toString());
        }
    }

    @Nested
    @DisplayName("Tests pingServers() method")
    class TestPingServers {
        CapacityFactorMonitor capFactorMonitor;
        int hashRingIndex;
        ArgumentCaptor<HttpUriRequest> argument;
        int backEndPort;
        JSONObject mockJsonResponse;
        RequestDecoder mockDecoder;
        CloseableHttpResponse mockResponse;

        @BeforeEach
        public void setup() throws IOException {
            this.hashRingIndex = 1_000;
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.argument = ArgumentCaptor.forClass(HttpGet.class);

            // set up canned response regarding the port of the server that the backend was started up on which is returned when a request is made through the
            // mock HttpClient to the (virtual) BackEndInitiator to start a server
            this.backEndPort = 1_000;
            this.mockJsonResponse = new JSONObject();
            this.mockJsonResponse.put("port", this.backEndPort);
            this.mockJsonResponse.put("capacity_factor", 0.5);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.BACKEND_INITIATOR_PORT, this.mockDecoder);
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);
            this.mockResponse = Mockito.mock(CloseableHttpResponse.class);
            when(CapacityFactorMonitorTest.this.mockClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockResponse);
            when(this.mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);

            // startup a server
            this.capFactorMonitor.startUpBackEnd(this.hashRingIndex);

            // run pingServer()
            try {
                this.capFactorMonitor.pingServers();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Test
        @DisplayName("Test that request is sent to backEndInitiator for update on capacity factor")
        public void shouldSendRequestWithPostMethod() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(1)).execute(any(HttpGet.class));
        }

        @Test
        @DisplayName("Request should be of type POST")
        public void shouldSendRequestToBackEndInitiator() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(2)).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(1);
            assertEquals("GET", request.getMethod());
        }

        @Test
        @DisplayName("Request should go to correct URI")
        public void requestShouldGoToCorrectURI() throws IOException {
            verify(CapacityFactorMonitorTest.this.mockClient, times(2)).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(1);
            assertEquals("http://127.0.0.1:" + this.backEndPort + "/capacity_factor", request.getURI().toString());
        }
    }

    @Nested
    @DisplayName("Tests selectPort() method")
    // test port selection logic based on given resource id
    class TestSelectPort {
        CapacityFactorMonitor capFactorMonitor;
        RequestDecoder mockDecoder;
        int initialBackEndPort;
        JSONObject mockJsonResponse;

        @BeforeEach
        public void setup() throws IOException {
            this.mockDecoder = Mockito.mock(RequestDecoder.class);
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.BACKEND_INITIATOR_PORT, this.mockDecoder);
            this.mockJsonResponse = new JSONObject();
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);
            // sets port of first backend server to 3_000
            this.initialBackEndPort = 3_000;
            this.mockJsonResponse.put("port", this.initialBackEndPort);
            when(this.mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);
        }

        @Test
        @DisplayName("When the next port is between position 0 and the initial position")
        public void nextPortCounterClockWise() {
            int hashRingIndex = 4_000;
            int resourceId = 105_000;
            int serverPort = this.capFactorMonitor.startUpBackEnd(hashRingIndex);
            int selectedPort = this.capFactorMonitor.selectPort(resourceId);
            assertEquals(serverPort, selectedPort);
        }

        @Test
        @DisplayName("When the next port is between the last position and the initial position")
        public void nextPortClockWise() {
            int hashRingIndex = 2_000;
            int resourceId = 105_000;
            int serverPort = this.capFactorMonitor.startUpBackEnd(hashRingIndex);
            int selectedPort = this.capFactorMonitor.selectPort(resourceId);
            assertEquals(serverPort, selectedPort);
        }

        @Test
        @DisplayName("When the next port is on the initial position")
        public void nextPortOnInitialPosition() {
            int hashRingIndex = 3_000;
            int resourceId = 105_000;
            int serverPort = this.capFactorMonitor.startUpBackEnd(hashRingIndex);
            int selectedPort = this.capFactorMonitor.selectPort(resourceId);
            assertEquals(serverPort, selectedPort);
        }
    }
}