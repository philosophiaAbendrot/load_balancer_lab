package loadbalancer.factory;

import loadbalancer.monitor.CapacityFactorMonitor;
import loadbalancer.util.RequestDecoder;

public interface CapacityFactorMonitorFactory {
    // produces and returns a CapacityFactorMonitor instance
    CapacityFactorMonitor produceCapacityFactorMonitor(ClientFactory clientFact, int backEndInitiatorPort, RequestDecoder decoder);
}
