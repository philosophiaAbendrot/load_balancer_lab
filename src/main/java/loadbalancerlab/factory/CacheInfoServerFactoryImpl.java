package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheInfoRequestHandler;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.ServerMonitorImpl;

public class CacheInfoServerFactoryImpl implements CacheInfoServerFactory {
    @Override
    public CacheInfoRequestHandler produceCacheInfoRequestHandler( ServerMonitorImpl serverMonitor) {
        return new CacheInfoRequestHandler(serverMonitor);
    }

    @Override
    public CacheInfoServerRunnable produceCacheInfoServerRunnable(int defaultPort, CacheInfoRequestHandler cacheInfoRequestHandler) {
        return new CacheInfoServerRunnable(defaultPort, cacheInfoRequestHandler);
    }
}