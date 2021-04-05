import loadbalancer.Client;
import loadbalancer.factory.HttpClientFactory;
import net.bytebuddy.asm.Advice;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.*;

import loadbalancer.services.ConstantDemandFunctionImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClientTest {
    Client client;
    HttpClientFactory mockHttpClientFactory;
    CloseableHttpClient mockHttpClient;
    int restInterval;
    Thread clientThread;

    @BeforeEach
    public void setup() {
        this.restInterval = 1_000;
        this.mockHttpClient = Mockito.mock(CloseableHttpClient.class);
        this.mockHttpClientFactory = Mockito.mock(HttpClientFactory.class);
        when(this.mockHttpClientFactory.buildApacheClient()).thenReturn(this.mockHttpClient);
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
            ClientTest.this.client = new Client("1", System.currentTimeMillis() + 20_000, new ConstantDemandFunctionImpl(ClientTest.this.restInterval), ClientTest.this.mockHttpClientFactory);
            ClientTest.this.clientThread = new Thread(ClientTest.this.client);
            ClientTest.this.clientThread.start();
        }

        @Test
        @DisplayName("Request should be sent to the right uri")
        public void testShouldSendToCorrectUri() {

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
