package loadbalancer.factory;

import loadbalancer.cacheserver.CacheServer;
import loadbalancer.services.monitor.RequestMonitor;

public class CacheServerFactoryImpl implements CacheServerFactory {
    public CacheServer produceCacheServer(RequestMonitor reqMonitor) {
        return new CacheServer(reqMonitor);
    }

    public Thread produceCacheServerThread(CacheServer cacheServer) {
        return new Thread(cacheServer);
    }
}