import loadbalancer.LoadBalancer;
import loadbalancer.factory.CapacityFactorMonitorFactory;
import loadbalancer.factory.ClientFactory;
import loadbalancer.factory.ClientFactoryImpl;
import loadbalancer.util.RequestDecoder;
import loadbalancer.services.monitor.CapacityFactorMonitor;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class LoadBalancerTest {
    static final int DEFAULT_BACKEND_PORT = 5050;
    int startUpServerCount;
    int backEndInitiatorPort;
    CapacityFactorMonitorFactory capacityFactorMonitorFactoryMock;
    CapacityFactorMonitor capacityFactorMonitorMock;
    int loadBalancerPort;
    Thread loadBalancerThread;
    LoadBalancer loadBalancer;

    @BeforeEach
    public void setup() {
        this.capacityFactorMonitorFactoryMock = Mockito.mock(CapacityFactorMonitorFactory.class);
        this.startUpServerCount = 10;
        this.backEndInitiatorPort = 8080;
        this.capacityFactorMonitorMock = Mockito.mock(CapacityFactorMonitor.class);
        when(this.capacityFactorMonitorMock.startUpBackEnd(anyInt())).thenReturn(DEFAULT_BACKEND_PORT);
        when(this.capacityFactorMonitorFactoryMock.produceCapacityFactorMonitor(any(ClientFactory.class), anyInt(), any(RequestDecoder.class))).thenReturn(this.capacityFactorMonitorMock);
    }

    @AfterEach
    public void reset() {
        // terminate thread
        this.loadBalancerThread.interrupt();
    }

    @Nested
    @DisplayName("Test initialization of LoadBalancer")
    public class TestInitialization {
        ClientFactory clientFactoryMock;

        @BeforeEach
        public void setup() {
            this.clientFactoryMock = new ClientFactoryImpl();
            LoadBalancerTest.this.loadBalancer = new LoadBalancer(LoadBalancerTest.this.startUpServerCount, LoadBalancerTest.this.backEndInitiatorPort, LoadBalancerTest.this.capacityFactorMonitorFactoryMock, this.clientFactoryMock);
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
            verify(LoadBalancerTest.this.capacityFactorMonitorFactoryMock, times(1)).produceCapacityFactorMonitor(any(ClientFactory.class), anyInt(), any(RequestDecoder.class));
        }

        @Test
        @DisplayName("load balancer should send requests to start up x backend server threads immediately following startup")
        public void loadBalancerShouldSpawnBackEndThreads() {
            verify(LoadBalancerTest.this.capacityFactorMonitorMock, times(LoadBalancerTest.this.startUpServerCount)).startUpBackEnd(anyInt());
        }
    }
}