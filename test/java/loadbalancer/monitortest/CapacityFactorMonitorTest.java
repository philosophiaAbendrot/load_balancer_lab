package loadbalancer.monitortest;

import loadbalancer.factory.ClientFactory;
import loadbalancer.monitor.CapacityFactorMonitor;
import loadbalancer.monitor.CapacityFactorMonitorImpl;
import loadbalancer.util.Logger;
import loadbalancer.util.RequestDecoderImpl;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CapacityFactorMonitorTest {
    // test that ping servers sends a request to existing backends

    public static class TestPingServers {
        CapacityFactorMonitor capFactorMonitor;
        ClientFactory clientFactory;
        CloseableHttpClient mockClient;
        long currentTime;
        int backEndInitiatorPort;
        int hashRingIndex;
        ArgumentCaptor<HttpUriRequest> argument;

        @BeforeEach
        public void setup() {
            this.clientFactory = Mockito.mock(ClientFactory.class);
            this.mockClient = Mockito.mock(CloseableHttpClient.class);
            this.currentTime = System.currentTimeMillis();
            this.backEndInitiatorPort = 3_000;
            this.hashRingIndex = 1_000;
            this.capFactorMonitor = new CapacityFactorMonitorImpl(this.clientFactory, this.currentTime, this.backEndInitiatorPort, new RequestDecoderImpl());
            this.argument = ArgumentCaptor.forClass(HttpUriRequest.class);
        }

        @Test
        @DisplayName("Test that request is sent to backEndInitiator for update on capacity factor")
        public void shouldSendRequestWithPostMethod() throws IOException {
            when(this.clientFactory.buildApacheClient()).thenReturn(this.mockClient);

            // startup a server
            this.capFactorMonitor.startUpBackEnd(this.hashRingIndex);

            try {
                this.capFactorMonitor.pingServers();
            } catch (IOException e) {
                e.printStackTrace();
            }

            verify(this.mockClient).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(0);
            assertEquals("POST", request.getMethod());
        }

        @Test
        @DisplayName("Should send request to port that BackEndInitiator is running on")
        public void shouldSendRequestToBackEndInitiator() throws IOException {
            when(this.clientFactory.buildApacheClient()).thenReturn(this.mockClient);

            // startup a server
            this.capFactorMonitor.startUpBackEnd(this.hashRingIndex);

            try {
                this.capFactorMonitor.pingServers();
            } catch (IOException e) {
                e.printStackTrace();
            }

            verify(this.mockClient).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(0);
            assertEquals("POST", request.getMethod());
        }
    }

    // test port selection logic based on given resource id
    public static class TestSelectPort {
        CapacityFactorMonitor capFactorMonitor;
        ClientFactory clientFactory;
        CloseableHttpClient mockClient;
        long currentTime;
        int backEndInitiatorPort;

        @BeforeEach
        public void setup() {
            Logger.configure(new String[] { "capacityModulation" });
            this.clientFactory = Mockito.mock(ClientFactory.class);
            this.mockClient = Mockito.mock(CloseableHttpClient.class);
            this.currentTime = System.currentTimeMillis();
            this.backEndInitiatorPort = 3_000;
            this.capFactorMonitor = new CapacityFactorMonitorImpl(this.clientFactory, this.currentTime, this.backEndInitiatorPort, new RequestDecoderImpl());
            when(this.clientFactory.buildApacheClient()).thenReturn(this.mockClient);
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

    // test that startUpBackEnd method sends a request to BackEndInitiator port to start up a server when called

    // test that startUpBackEnd records current time and portInt that the new server starts up on
}