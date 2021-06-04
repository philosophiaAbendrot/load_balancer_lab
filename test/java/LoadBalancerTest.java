import loadbalancerlab.loadbalancer.LoadBalancer;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.shared.Logger;

import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class LoadBalancerTest {
    static final int DEFAULT_CACHE_SERVER_PORT = 5050;
    int startUpServerCount;
    int cacheServerManagerPort;
    int loadBalancerPort;
    Thread loadBalancerThread;
    LoadBalancer loadBalancer;

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
    }

    @BeforeEach
    public void setup() {
        this.startUpServerCount = 10;
        this.cacheServerManagerPort = 8080;
    }

    @AfterEach
    public void reset() {
        // terminate thread
        this.loadBalancerThread.interrupt();
    }

    @Nested
    @DisplayName("Test initialization of LoadBalancer")
    public class TestInitialization {
        HttpClientFactory clientFactoryMock;

        @BeforeEach
        public void setup() {
            this.clientFactoryMock = new HttpClientFactoryImpl();
            LoadBalancerTest.this.loadBalancer = new LoadBalancer(LoadBalancerTest.this.startUpServerCount, LoadBalancerTest.this.cacheServerManagerPort, this.clientFactoryMock);
            LoadBalancerTest.this.loadBalancerThread = new Thread(LoadBalancerTest.this.loadBalancer);

            LoadBalancerTest.this.loadBalancerThread.start();
            int port = -1;

            // wait for load balancer thread to initialize
            while (port == -1) {
                port = LoadBalancerTest.this.loadBalancer.getPort();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {

                }
            }

            LoadBalancerTest.this.loadBalancerPort = port;
        }
    }
}