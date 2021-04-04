import loadbalancer.LoadBalancer;
import loadbalancer.factory.CapacityFactorMonitorFactory;
import loadbalancer.factory.ClientFactory;
import loadbalancer.util.RequestDecoder;
import loadbalancer.monitor.CapacityFactorMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class LoadBalancerTest {
    // check that load balancer starts up a CapacityFactorImpl instance
    LoadBalancer loadBalancer;
    int startUpServerCount;
    int backEndInitiatorPort;
    Thread loadBalancerThread;
    CapacityFactorMonitorFactory capFactMonitorFactMock;
    CapacityFactorMonitor capFactorMonitorMock;

    @BeforeEach
    public void setup() {
        this.capFactMonitorFactMock = Mockito.mock(CapacityFactorMonitorFactory.class);
        this.startUpServerCount = 10;
        this.backEndInitiatorPort = 8080;
        this.loadBalancer = new LoadBalancer(this.startUpServerCount, this.backEndInitiatorPort, this.capFactMonitorFactMock);
        this.loadBalancerThread = new Thread(loadBalancer);
        this.capFactorMonitorMock = Mockito.mock(CapacityFactorMonitor.class);

        when(capFactMonitorFactMock.produceCapacityFactorMonitor(any(ClientFactory.class), anyInt(), any(RequestDecoder.class))).thenReturn(this.capFactorMonitorMock);
        // start load balancer thread
        this.loadBalancerThread.start();
    }

    // check that load balancer sends requests to start up x backend server threads immediately following startup

    // test that requests to LoadBalancer are passed on to BackEnds
}