import loadbalancer.LoadBalancer;
import loadbalancer.factory.CapacityFactorMonitorFactory;
import loadbalancer.factory.ClientFactory;
import loadbalancer.util.RequestDecoder;
import loadbalancer.monitor.CapacityFactorMonitor;

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
    Thread loadBalancerThread;
    CapacityFactorMonitorFactory capacityFactorMonitorFactoryMock;
    CapacityFactorMonitor capacityFactorMonitorMock;
    LoadBalancer loadBalancer;
    int loadBalancerPort;

    @BeforeEach
    public void setup() {
        this.capacityFactorMonitorFactoryMock = Mockito.mock(CapacityFactorMonitorFactory.class);
        this.startUpServerCount = 10;
        this.backEndInitiatorPort = 8080;
        this.capacityFactorMonitorMock = Mockito.mock(CapacityFactorMonitor.class);
        when(this.capacityFactorMonitorMock.startUpBackEnd(anyInt())).thenReturn(DEFAULT_BACKEND_PORT);
        when(this.capacityFactorMonitorFactoryMock.produceCapacityFactorMonitor(any(ClientFactory.class), anyInt(), any(RequestDecoder.class))).thenReturn(this.capacityFactorMonitorMock);
        this.loadBalancer = new LoadBalancer(this.startUpServerCount, this.backEndInitiatorPort, this.capacityFactorMonitorFactoryMock);
        this.loadBalancerThread = new Thread(this.loadBalancer);

        // start load balancer thread
        this.loadBalancerThread.start();
        int port = -1;

        // wait for load balancer thread to initialize
        while (port == -1) {
            port = this.loadBalancer.getPort();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {

            }
        }
        this.loadBalancerPort = port;
    }

    @AfterEach
    public void reset() {
        // terminate thread
        LoadBalancerTest.this.loadBalancerThread.interrupt();
    }

    @Nested
    @DisplayName("Test initialization of LoadBalancer")
    public class TestInitialization {
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

    @Nested
    @DisplayName("Test request redirection")
    public class TestRequestRedirection {
        @Test
        @DisplayName("load balancer should redirect incoming request")
        public void loadBalancerShouldRedirectIncomingRequest() {

        }
    }


    @Test
    @DisplayName("test that requests to LoadBalancer are passed on to BackEnds")
    public void testRequestsToLoadBalancerPassedOnToBackEnds() {

    }
}