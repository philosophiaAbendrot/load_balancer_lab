    package loadbalancerlab.factory;

import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.services.monitor.RequestMonitor;

public interface CacheServerFactory {
    // produces and returns a cache server instance given a request monitor
    CacheServer produceCacheServer(RequestMonitor reqMonitor);

    // produces and returns a cache server thread instance given a backend instance
    Thread produceCacheServerThread(CacheServer cacheServer);
}
