package loadbalancerlab.client;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ClientManagerRunnableTest {
    ClientManagerRunnable manager;
    HttpClientFactory mockClientFactory;
    Thread clientManagerThread;
    int numClients;

    @BeforeEach
    public void setup() {
        numClients = 10;
        Config config = new Config();
        config.setNumClients(numClients);
        ClientManagerRunnable.configure(config);
        mockClientFactory = Mockito.mock(HttpClientFactory.class);
        manager = new ClientManagerRunnable(mockClientFactory);
        clientManagerThread = new Thread(manager);
        clientManagerThread.start();
    }

    @AfterEach
    public void shutdown() {
        clientManagerThread.interrupt();
    }

    @Nested
    @DisplayName("Test client startup")
    class TestClientStartup {
        @Test
        @DisplayName("Client manager should generate clients")
        public void shouldGenerateClients() {
            verify(mockClientFactory, times(numClients)).buildApacheClient();
        }

        @Test
        @DisplayName("Client manager should generate threads for clients")
        public void shouldGenerateClientThreads() {
            assertEquals(numClients, manager.clientThreads.size());
        }
    }


    @Nested
    @DisplayName("Test client shutdown")
    class TestClientShutdown {
        Thread mockClientThread;

        @BeforeEach
        public void setup() {
            manager.clientThreads = new ArrayList<>();
            mockClientThread = Mockito.mock(Thread.class);

            for (int i = 0; i < numClients; i++) {
                manager.clientThreads.add(mockClientThread);
            }

            clientManagerThread.interrupt();
        }

        @Test
        @DisplayName("ClientManager should shut down all client threads when shutting down")
        public void shouldShutdownAllClientThreads() {
            verify(mockClientThread, times(numClients)).interrupt();
        }
    }
}