package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;

public class CacheRedistributorRunnable implements Runnable {
    CacheRedistributor cacheRedis;
    static int pingInterval;
    static int cacheRemapInterval;
    static int hashRingAngleRecordInterval;

    public static void configure( Config config ) {
        pingInterval = config.getCacheRedisPingInterval();
        cacheRemapInterval = config.getCacheRedisRemapInterval();
        hashRingAngleRecordInterval = config.getHashRingAngleRecordInterval();
    }

    public CacheRedistributorRunnable( CacheRedistributor _cacheRedis ) {
        cacheRedis = _cacheRedis;
    }

    @Override
    public void run() {
        int currentTime = (int)(System.currentTimeMillis() / 1_000);
        int lastRequestServerTime = currentTime;
        int lastRemapTime = currentTime;
        int lastHashRingRecordTime = currentTime;

        while (true) {
            currentTime = (int)(System.currentTimeMillis() / 1_000);

            if (currentTime - lastRequestServerTime >= pingInterval)
                cacheRedis.requestServerInfo();

            currentTime = (int)(System.currentTimeMillis() / 1_000);

            if (currentTime - lastRemapTime >= cacheRemapInterval)
                cacheRedis.remapCacheKeys();

            currentTime = (int)(System.currentTimeMillis() / 1_000);

            if (currentTime - lastHashRingRecordTime >= hashRingAngleRecordInterval) {
                cacheRedis.recordServerAngles(currentTime);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}