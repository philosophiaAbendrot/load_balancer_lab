import loadbalancer.Client;
import loadbalancer.factory.HttpClientFactory;
import loadbalancer.util.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.*;

import loadbalancer.services.ConstantDemandFunctionImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClientTest {
    Client client;
    HttpClientFactory mockHttpClientFactory;
    CloseableHttpClient mockHttpClient;
    int restInterval;
    long requestStartTime;
    Thread clientThread;
    int loadBalancerPort = 8080;
    Random rand;
    int resourceId;

    @BeforeEach
    public void setup() {
        this.rand = new Random();
        Logger.configure(new Logger.LogType[] { Logger.LogType.THREAD_MANAGEMENT, Logger.LogType.CLIENT_STARTUP });
        this.restInterval = 100;
        this.mockHttpClient = Mockito.mock(CloseableHttpClient.class);
        this.mockHttpClientFactory = Mockito.mock(HttpClientFactory.class);
        // allow requests to be sent immediately after Client startup for testing purposes
        this.requestStartTime = System.currentTimeMillis() - 1_000;
        this.client.setLoadBalancerPort(this.loadBalancerPort);
        when(this.mockHttpClientFactory.buildApacheClient()).thenReturn(this.mockHttpClient);
        this.resourceId = rand.nextInt(10_000);
    }

    @AfterEach
    public void reset() {
        this.clientThread.interrupt();
    }

    @Nested
    @DisplayName("Test request sent to Load Balancer")
    public class TestRequestSentToLoadBalancer {
        CloseableHttpResponse mockCloseableHttpResponse;
        ArgumentCaptor<HttpUriRequest> requestCaptor;

        @BeforeEach
        public void setup() throws IOException {
            this.requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            this.mockCloseableHttpResponse = Mockito.mock(CloseableHttpResponse.class);
            when(ClientTest.this.mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockCloseableHttpResponse);

            ClientTest.this.client = new Client("1", System.currentTimeMillis() + 20_000, new ConstantDemandFunctionImpl(ClientTest.this.restInterval), ClientTest.this.mockHttpClientFactory, ClientTest.this.requestStartTime, ClientTest.this.resourceId);
            ClientTest.this.clientThread = new Thread(ClientTest.this.client);
            ClientTest.this.clientThread.start();

            // wait for client to initialize and send request
            try {
                Thread.sleep(ClientTest.this.restInterval * 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Test
        @DisplayName("Client should create http client using provided factory")
        public void clientShouldCreateHttpClient() throws IOException {
            verify(ClientTest.this.mockHttpClientFactory, times(1)).buildApacheClient();
        }

        @Test
        @DisplayName("Request should be sent to the right uri")
        public void testShouldSendToCorrectUri() throws IOException {
            verify(ClientTest.this.mockHttpClient, times(1)).execute(this.requestCaptor.capture());
            HttpUriRequest request = this.requestCaptor.getAllValues().get(0);
            String expectedPath = "http://127.0.0.1:" + ClientTest.this.loadBalancerPort + "/api/" + ClientTest.this.resourceId;
            assertEquals(expectedPath, request.getURI().toString());
        }

        @Test
        @DisplayName("Request should be of type GET")
        public void testShouldHaveCorrectMethod() throws IOException {
            verify(ClientTest.this.mockHttpClient, times(1)).execute(this.requestCaptor.capture());
            HttpUriRequest request = this.requestCaptor.getAllValues().get(0);
            assertEquals("GET", request.getMethod().toString());
        }
    }
}
