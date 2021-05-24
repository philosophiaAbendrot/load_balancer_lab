import loadbalancer.util.RequestDecoder;
import loadbalancer.util.RequestDecoderImpl;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.apache.http.impl.client.HttpClients;
import org.mockito.Mockito;

import loadbalancer.BackEndInitiator;
import loadbalancer.factory.BackEndFactoryImpl;
import loadbalancer.factory.BackEndFactory;
import loadbalancer.services.monitor.RequestMonitor;
import loadbalancer.BackEnd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BackEndInitiatorTest {
    @Nested
    @DisplayName("Testing with a mock backend")
    public class MockBackEndTests {
        BackEndFactory mockFactory;
        BackEnd mockBackEnd;
        Thread mockBackEndThread;
        BackEndInitiator initiator;
        Thread initiatorThread;
        int backEndInitiatorPort;

        @BeforeEach
        public void setup() {
            this.mockFactory = Mockito.mock(BackEndFactoryImpl.class);
            this.mockBackEnd = Mockito.mock(BackEnd.class);
            this.mockBackEnd.port = 37_100;
            this.mockBackEndThread = Mockito.mock(Thread.class);

            when(this.mockFactory.produceBackEnd(any(RequestMonitor.class))).thenReturn(mockBackEnd);
            when(this.mockFactory.produceBackEndThread(any(BackEnd.class))).thenReturn(this.mockBackEndThread);

            this.initiator = new BackEndInitiator(this.mockFactory);
            this.initiatorThread = new Thread(this.initiator);
            this.initiatorThread.start();
            this.backEndInitiatorPort = BackEndInitiatorTest.waitUntilServerReady(this.initiator);
        }

        @Test
        @DisplayName("BackEndInitiator should start up a BackEnd instance when sent a request telling it to do so")
        public void BackEndInitiatorShouldStartBackEndInstance() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost req = new HttpPost("http://127.0.0.1:" + this.backEndInitiatorPort + "/backends");

            try {
                CloseableHttpResponse response = client.execute(req);
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.initiatorThread.interrupt();

            verify(this.mockFactory, times(1)).produceBackEnd(any(RequestMonitor.class));
            verify(this.mockFactory, times(1)).produceBackEndThread(any(BackEnd.class));
        }

        @Test
        @DisplayName("When BackEndInitiator thread is interrupted, it interrupts all backend servers that it has spawned")
        public void BackEndInitiatorThreadInterruptedInterruptsAllBackEndServers() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost req = new HttpPost("http://127.0.0.1:" + this.backEndInitiatorPort + "/backends");

            // send request to server and wait for it to be received
            try {
                CloseableHttpResponse response = client.execute(req);
                client.close();
                Thread.sleep(100);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // interrupt BackEndInitiator thread
            this.initiatorThread.interrupt();

            // wait for BackEndInitiator to run interruption callbacks
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }

            // verify that BackEndThread has been interrupted
            verify(this.mockBackEndThread, times(1)).interrupt();
        }
    }

    @Nested
    @DisplayName("Testing with a live backend")
    public class LiveBackEndTests {
        BackEndFactory factory;
        BackEnd backEnd;
        Thread backEndThread;
        BackEndInitiator initiator;
        Thread initiatorThread;
        int backEndInitiatorPort;
        int backEndPort;

        @BeforeEach
        public void setup() {
            this.factory = new BackEndFactoryImpl();
            this.initiator = new BackEndInitiator(this.factory);
            this.initiatorThread = new Thread(this.initiator);
            this.initiatorThread.start();
            this.backEndInitiatorPort = BackEndInitiatorTest.waitUntilServerReady(this.initiator);
            startServerAndGetPort();
        }

        private void startServerAndGetPort() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost req = new HttpPost("http://127.0.0.1:" + this.backEndInitiatorPort + "/backends");
            this.backEndPort = -1;

            try {
                CloseableHttpResponse response = client.execute(req);
                RequestDecoder decoder = new RequestDecoderImpl();
                JSONObject jsonObj = decoder.extractJsonApacheResponse(response);
                int portInt = jsonObj.getInt("port");
                HttpEntity responseBody = response.getEntity();
                InputStream responseStream = responseBody.getContent();
                String responseString = IOUtils.toString(responseStream, StandardCharsets.UTF_8.name());
                response.close();
                responseStream.close();
                this.backEndPort = portInt;
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Test
        @DisplayName("When a new BackEnd server is started, it should return the port that the new backend server is running on")
        public void backEndServerStartedReturnPort() {
            // send request to port returned by the request to see if the server is active
            CloseableHttpClient client = HttpClients.createDefault();
            int status = -1;

            if (this.backEndPort != -1) {
                try {
                    HttpGet getRequest = new HttpGet("http://127.0.0.1:" + this.backEndPort);
                    CloseableHttpResponse response = client.execute(getRequest);
                    status = response.getStatusLine().getStatusCode();
                } catch (IOException e) { e.printStackTrace(); }
            }

            this.initiatorThread.interrupt();

            assertEquals(status, 200);
        }

        @Test
        @DisplayName("When a request is sent to shut down a certain server, that server should be shut down")
        public void shutDownSpecificServer() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpDelete req = new HttpDelete("http://127.0.0.1:" + this.backEndInitiatorPort + "/backend/" + this.backEndPort);

            try {
                CloseableHttpResponse response = client.execute(req);
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            HttpGet getReq = new HttpGet("http://127.0.0.1:" + this.backEndPort);
            final CloseableHttpClient client2 = HttpClients.createDefault();

            Exception exception = assertThrows(IOException.class, () -> {
               CloseableHttpResponse response = client2.execute(getReq);
            });
        }
    }

    // waits until a server has started up
    // returns port
    private static int waitUntilServerReady(BackEndInitiator initiator) {
        int backEndInitiatorPort = initiator.getPort();

        while (backEndInitiatorPort == -1) {
            try {
                Thread.sleep(20);
                backEndInitiatorPort = initiator.getPort();
            } catch (InterruptedException e) { }
        }

        return backEndInitiatorPort;
    }
}