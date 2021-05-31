package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheInfoRequestHandler;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.ServerMonitorImpl;

public interface CacheInfoServerFactory {
    CacheInfoRequestHandler produceCacheInfoRequestHandler( ServerMonitorImpl serverMonitor );

    CacheInfoServerRunnable produceCacheInfoServerRunnable( int defaultPort, CacheInfoRequestHandler cacheInfoRequestHandler );
}
