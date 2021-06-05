package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.ConfigImpl;

public class CacheRedistributorRunnable implements Runnable {
    CacheRedistributor cacheRedis;
    static int pingInterval;
    static int cacheRemapInterval;

    public static void configure( ConfigImpl config ) {
        pingInterval = config.getCacheRedisPingInterval();
        cacheRemapInterval = config.getCacheRedisRemapInterval();
    }

    public CacheRedistributorRunnable( CacheRedistributor _cacheRedis ) {
        cacheRedis = _cacheRedis;
    }

    @Override
    public void run() {
        int currentTime = (int)(System.currentTimeMillis() / 1_000);
        int lastRequestServerTime = currentTime;
        int lastRemapTime = currentTime;

        while (true) {
            currentTime = (int)(System.currentTimeMillis() / 1_000);

            if (currentTime - lastRequestServerTime >= pingInterval)
                cacheRedis.requestServerInfo();

            if (currentTime - lastRemapTime >= cacheRemapInterval)
                cacheRedis.remapCacheKeys();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}