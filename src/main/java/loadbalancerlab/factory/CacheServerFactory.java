package loadbalancerlab.factory;

import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.cacheserver.RequestMonitor;

/**
 * A factory class which produces CacheServer instances.
 */
public class CacheServerFactory {

    /**
     * Produces CacheServer instance given a RequestMonitor instance.
     * @param reqMonitor    A RequestMonitor instance which monitors and processes telemetry data for a CacheServer instance.
     * @return:             CacheServer instance used to process HTTP requests from Client instances.
     */
    public CacheServer produceCacheServer(RequestMonitor reqMonitor) {
        return new CacheServer(reqMonitor);
    }

    /**
     * Produces a Thread which runs a CacheServer given a CacheServer instance.
     * @param cacheServer   CacheServer instance used to process HTTP requests from Client instances.
     * @return              A Thread which runs the associated CacheServer instance.
     */
    public Thread produceCacheServerThread(CacheServer cacheServer) {
        return new Thread(cacheServer);
    }
}