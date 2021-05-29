package loadbalancerlab.factory;

import loadbalancerlab.services.monitor.CapacityFactorMonitor;
import loadbalancerlab.shared.RequestDecoder;

public interface CapacityFactorMonitorFactory {
    // produces and returns a CapacityFactorMonitor instance
    CapacityFactorMonitor produceCapacityFactorMonitor(HttpClientFactory clientFact, int cacheServerManagerPort, RequestDecoder decoder);
}
