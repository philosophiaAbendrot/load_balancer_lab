package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheInfoRequestHandler;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.ServerMonitor;

public interface CacheInfoServerFactory {
    CacheInfoRequestHandler produceCacheInfoRequestHandler( ServerMonitor serverMonitor );

    CacheInfoServerRunnable produceCacheInfoServerRunnable( int defaultPort, CacheInfoRequestHandler cacheInfoRequestHandler );
}
