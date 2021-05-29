import loadbalancerlab.loadbalancer.LoadBalancer;
import loadbalancerlab.factory.CapacityFactorMonitorFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoder;
import loadbalancerlab.services.monitor.CapacityFactorMonitor;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class LoadBalancerTest {
    static final int DEFAULT_CACHE_SERVER_PORT = 5050;
    int startUpServerCount;
    int cacheServerManagerPort;
    CapacityFactorMonitorFactory capacityFactorMonitorFactoryMock;
    CapacityFactorMonitor capacityFactorMonitorMock;
    int loadBalancerPort;
    Thread loadBalancerThread;
    LoadBalancer loadBalancer;

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
    }

    @BeforeEach
    public void setup() {
        this.capacityFactorMonitorFactoryMock = Mockito.mock(CapacityFactorMonitorFactory.class);
        this.startUpServerCount = 10;
        this.cacheServerManagerPort = 8080;
        this.capacityFactorMonitorMock = Mockito.mock(CapacityFactorMonitor.class);
        when(this.capacityFactorMonitorMock.startupCacheServer(anyInt())).thenReturn(DEFAULT_CACHE_SERVER_PORT);
        when(this.capacityFactorMonitorFactoryMock.produceCapacityFactorMonitor(any(HttpClientFactory.class), anyInt(), any(RequestDecoder.class))).thenReturn(this.capacityFactorMonitorMock);
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
            LoadBalancerTest.this.loadBalancer = new LoadBalancer(LoadBalancerTest.this.startUpServerCount, LoadBalancerTest.this.cacheServerManagerPort, LoadBalancerTest.this.capacityFactorMonitorFactoryMock, this.clientFactoryMock);
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

        @Test
        @DisplayName("load balancer should initialize a CapacityFactorImpl instance")
        public void loadBalancerShouldInitializeCapacityFactorImpl() {
            verify(LoadBalancerTest.this.capacityFactorMonitorFactoryMock, times(1)).produceCapacityFactorMonitor(any(HttpClientFactory.class), anyInt(), any(RequestDecoder.class));
        }

        @Test
        @DisplayName("load balancer should send requests to start up x cache server threads immediately following startup")
        public void loadBalancerShouldSpawnCacheServerThreads() {
            verify(LoadBalancerTest.this.capacityFactorMonitorMock, times(LoadBalancerTest.this.startUpServerCount)).startupCacheServer(anyInt());
        }
    }
}