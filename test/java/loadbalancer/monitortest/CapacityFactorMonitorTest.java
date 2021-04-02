package loadbalancer.monitortest;

import loadbalancer.factory.ClientFactory;
import loadbalancer.monitor.CapacityFactorMonitor;
import loadbalancer.monitor.CapacityFactorMonitorImpl;
import loadbalancer.util.Logger;
import loadbalancer.util.RequestDecoder;
import loadbalancer.util.RequestDecoderImpl;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class CapacityFactorMonitorTest {
    // test that ping servers sends a request to existing backends
    private static final int BACKEND_INITIATOR_PORT = 8080;
    ClientFactory clientFactory;
    CloseableHttpClient mockClient;
    long currentTime;

    @BeforeEach
    public void setup() {
        this.clientFactory = Mockito.mock(ClientFactory.class);
        this.mockClient = Mockito.mock(CloseableHttpClient.class);
        this.currentTime = System.currentTimeMillis();
    }

    @Nested
    @DisplayName("Tests pingServers() method")
    class TestPingServers {
        CapacityFactorMonitor capFactorMonitor;
        int hashRingIndex;
        ArgumentCaptor<HttpUriRequest> argument;

        @BeforeEach
        public void setup() {
            CapacityFactorMonitorTest.this.currentTime = System.currentTimeMillis();
            this.hashRingIndex = 1_000;
            this.capFactorMonitor = new CapacityFactorMonitorImpl(CapacityFactorMonitorTest.this.clientFactory, CapacityFactorMonitorTest.this.currentTime, CapacityFactorMonitorTest.BACKEND_INITIATOR_PORT, new RequestDecoderImpl());
            this.argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        }

        @Test
        @DisplayName("Test that request is sent to backEndInitiator for update on capacity factor")
        public void shouldSendRequestWithPostMethod() throws IOException {
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);

            // startup a server
            this.capFactorMonitor.startUpBackEnd(this.hashRingIndex);

            try {
                this.capFactorMonitor.pingServers();
            } catch (IOException e) {
                e.printStackTrace();
            }

            verify(CapacityFactorMonitorTest.this.mockClient).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(0);
            assertEquals("POST", request.getMethod());
        }

        @Test
        @DisplayName("Should send request to port that BackEndInitiator is running on")
        public void shouldSendRequestToBackEndInitiator() throws IOException {
            when(CapacityFactorMonitorTest.this.clientFactory.buildApacheClient()).thenReturn(CapacityFactorMonitorTest.this.mockClient);

            // startup a server
            this.capFactorMonitor.startUpBackEnd(this.hashRingIndex);

            try {
                this.capFactorMonitor.pingServers();
            } catch (IOException e) {
                e.printStackTrace();
            }

            verify(CapacityFactorMonitorTest.this.mockClient).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(0);
            assertEquals("POST", request.getMethod());
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
            Logger.configure(new String[] { "capacityModulation" });
            CapacityFactorMonitorTest.this.clientFactory = Mockito.mock(ClientFactory.class);
            CapacityFactorMonitorTest.this.mockClient = Mockito.mock(CloseableHttpClient.class);
            CapacityFactorMonitorTest.this.currentTime = System.currentTimeMillis();
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

    @Nested
    @DisplayName("Tests startUpBackEnd() method")
    // test that startUpBackEnd method sends a request to BackEndInitiator port to start up a server when called
    class TestStartupBackEnd {
        CapacityFactorMonitor capFactorMonitor;
        RequestDecoder mockDecoder;
        int initialBackEndPort;
        JSONObject mockJsonResponse;

        @Test
        @DisplayName("Test that a request is sent to BackEndInitiator")
        public void testRequestSentBackEndInitiator() {


        }
    }

    // test that startUpBackEnd records current time and portInt that the new server starts up on
}