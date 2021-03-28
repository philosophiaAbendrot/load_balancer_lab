package loadbalancer.monitortest;

import loadbalancer.factory.ClientFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.Mockito;

import java.io.Closeable;

public class CapacityFactorMonitorTest {
    // test that ping servers sends a request to existing backends
    public static class TestPingServers {
        ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        when(clientFactory.buildApacheClient()).thenReturn(mockClient);
    }

    // test that capacity factor received is recorded

    // test port selection logic based on given resource id

    // test that startUpBackEnd method sends a request to BackEndInitiator port to start up a server when called

    // test that startUpBackEnd records current time and portInt that the new server starts up on
}