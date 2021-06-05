package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheInfoRequestHandler;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.ServerMonitor;

public class CacheInfoServerFactory {
    public CacheInfoRequestHandler produceCacheInfoRequestHandler( ServerMonitor serverMonitor) {
        return new CacheInfoRequestHandler(serverMonitor);
    }
    public CacheInfoServerRunnable produceCacheInfoServerRunnable(int defaultPort, CacheInfoRequestHandler cacheInfoRequestHandler) {
        return new CacheInfoServerRunnable(defaultPort, cacheInfoRequestHandler);
    }
}