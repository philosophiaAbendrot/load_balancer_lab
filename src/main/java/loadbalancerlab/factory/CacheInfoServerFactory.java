package loadbalancerlab.factory;

import loadbalancerlab.cacheservermanager.CacheInfoRequestHandler;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.ServerMonitor;

/**
 * Factory class for producing CacheInfoRequestHandler and CacheInfoServerRunnable instances
 */
public class CacheInfoServerFactory {
    /**
     * Given a ServerMonitor instance, produces a CacheInfoRequestHandler instance
     * @param serverMonitor: ServerMonitor instance for recording and processing CacheServer telemetry
     * @return: A CacheInfoRequestHandler instance which handles requests on updates of CacheServer capacity factor
     * values
     */
    public CacheInfoRequestHandler produceCacheInfoRequestHandler( ServerMonitor serverMonitor) {
        return new CacheInfoRequestHandler(serverMonitor);
    }

    /**
     *
     * @param cacheInfoRequestHandler
     * @return
     */
    public CacheInfoServerRunnable produceCacheInfoServerRunnable(CacheInfoRequestHandler cacheInfoRequestHandler) {
        return new CacheInfoServerRunnable(cacheInfoRequestHandler);
    }
}