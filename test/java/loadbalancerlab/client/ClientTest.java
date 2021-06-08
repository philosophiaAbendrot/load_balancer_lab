package loadbalancerlab.client;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.*;

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

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
    }

    @BeforeEach
    public void setup() {
        rand = new Random();
        restInterval = 100;
        mockHttpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClientFactory = Mockito.mock(HttpClientFactory.class);
        // allow requests to be sent immediately after Client startup for testing purposes
        requestStartTime = System.currentTimeMillis() - 1_000;
        client.setLoadBalancerPort(loadBalancerPort);
        when(mockHttpClientFactory.buildApacheClient()).thenReturn(mockHttpClient);
    }

    @AfterEach
    public void reset() {
        clientThread.interrupt();
    }

    @Nested
    @DisplayName("Test request sent to Load Balancer")
    public class TestRequestSentToLoadBalancer {
        CloseableHttpResponse mockCloseableHttpResponse;
        ArgumentCaptor<HttpUriRequest> requestCaptor;

        @BeforeEach
        public void setup() throws IOException {
            requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            mockCloseableHttpResponse = Mockito.mock(CloseableHttpResponse.class);
            when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(mockCloseableHttpResponse);

            client = new Client(System.currentTimeMillis() + 20_000, new ConstantDemandFunctionImpl(restInterval), mockHttpClientFactory, requestStartTime);
            clientThread = new Thread(client);
            clientThread.start();

            // wait for client to initialize and send request
            try {
                Thread.sleep(restInterval * 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Test
        @DisplayName("Client should create http client using provided factory")
        public void clientShouldCreateHttpClient() {
            verify(mockHttpClientFactory, times(1)).buildApacheClient();
        }

        @Test
        @DisplayName("Request should be sent to the right uri")
        public void testShouldSendToCorrectUri() throws IOException {
            verify(mockHttpClient, times(1)).execute(requestCaptor.capture());
            HttpUriRequest request = requestCaptor.getAllValues().get(0);
            String expectedPath = "http://127.0.0.1:" + loadBalancerPort + "/api/" + client.resourceName;
            assertEquals(expectedPath, request.getURI().toString());
        }

        @Test
        @DisplayName("Request should be of type GET")
        public void testShouldHaveCorrectMethod() throws IOException {
            verify(mockHttpClient, times(1)).execute(requestCaptor.capture());
            HttpUriRequest request = requestCaptor.getAllValues().get(0);
            assertEquals("GET", request.getMethod().toString());
        }
    }
}
