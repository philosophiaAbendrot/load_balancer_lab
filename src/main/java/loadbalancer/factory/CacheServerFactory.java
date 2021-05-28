package loadbalancer.factory;

import loadbalancer.cacheserver.CacheServer;
import loadbalancer.services.monitor.RequestMonitor;

public interface CacheServerFactory {
    // produces and returns a cache server instance given a request monitor
    CacheServer produceCacheServer(RequestMonitor reqMonitor);

    // produces and returns a cache server thread instance given a backend instance
    Thread produceCacheServerThread(CacheServer cacheServer);
}
