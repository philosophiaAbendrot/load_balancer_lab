package loadbalancerlab.factory;

import loadbalancerlab.services.monitor.CapacityFactorMonitor;
import loadbalancerlab.services.monitor.CapacityFactorMonitorImpl;
import loadbalancerlab.shared.RequestDecoder;
import loadbalancerlab.shared.RequestDecoderImpl;

public class CapacityFactorMonitorFactoryImpl implements CapacityFactorMonitorFactory {
    @Override
    public CapacityFactorMonitor produceCapacityFactorMonitor(HttpClientFactory clientFact, int cacheServerManagerPort, RequestDecoder decoder ) {
        return new CapacityFactorMonitorImpl(new HttpClientFactoryImpl(), System.currentTimeMillis(), cacheServerManagerPort, new RequestDecoderImpl());
    }
}