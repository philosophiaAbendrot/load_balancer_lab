package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheInfoRequestHandler;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.ServerMonitor;

public class CacheInfoServerFactoryImpl implements CacheInfoServerFactory {
    @Override
    public CacheInfoRequestHandler produceCacheInfoRequestHandler( ServerMonitor serverMonitor) {
        return new CacheInfoRequestHandler(serverMonitor);
    }

    @Override
    public CacheInfoServerRunnable produceCacheInfoServerRunnable(int defaultPort, CacheInfoRequestHandler cacheInfoRequestHandler) {
        return new CacheInfoServerRunnable(defaultPort, cacheInfoRequestHandler);
    }
}