package loadbalancer.factory;

import loadbalancer.services.monitor.CapacityFactorMonitor;
import loadbalancer.services.monitor.CapacityFactorMonitorImpl;
import loadbalancer.util.RequestDecoder;
import loadbalancer.util.RequestDecoderImpl;

public class CapacityFactorMonitorFactoryImpl implements CapacityFactorMonitorFactory {
    @Override
    public CapacityFactorMonitor produceCapacityFactorMonitor( ClientFactory clientFact, int backEndInitiatorPort, RequestDecoder decoder ) {
        return new CapacityFactorMonitorImpl(new ClientFactoryImpl(), System.currentTimeMillis(), backEndInitiatorPort, new RequestDecoderImpl());
    }
}