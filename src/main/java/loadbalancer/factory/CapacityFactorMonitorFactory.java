package loadbalancer.factory;

import loadbalancer.services.monitor.CapacityFactorMonitor;
import loadbalancer.util.RequestDecoder;

public interface CapacityFactorMonitorFactory {
    // produces and returns a CapacityFactorMonitor instance
    CapacityFactorMonitor produceCapacityFactorMonitor(HttpClientFactory clientFact, int cacheServerManagerPort, RequestDecoder decoder);
}
