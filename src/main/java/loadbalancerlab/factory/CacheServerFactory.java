package loadbalancerlab.factory;

import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.services.monitor.RequestMonitor;

public class CacheServerFactory {
    public CacheServer produceCacheServer(RequestMonitor reqMonitor) {
        return new CacheServer(reqMonitor);
    }

    public Thread produceCacheServerThread(CacheServer cacheServer) {
        return new Thread(cacheServer);
    }
}