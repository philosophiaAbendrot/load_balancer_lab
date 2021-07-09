package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;

/**
 * A Runnable implementation which wraps around the CacheRedistributor class and periodically calls methods on it
 */
public class CacheRedistributorRunnable implements Runnable {
    /**
     * Associated CacheRedistributor logic which manages a HashRing instance and handles consistent hashing logic to
     * delegate client request to CacheServer instances
     */
    CacheRedistributor cacheRedis;
    /**
     * Controls minimum interval (in milliseconds) between calls of CacheRedistributor.requestServerInfo() method.
     * This controls how often a request is for an update on telemetry for CacheServer instances.
     */
    static int pingInterval;
    /**
     * Controls minimum interval (in milliseconds) between calls of CacheRedistributor.remapCacheKeys() method.
     * This controls how often the resource mappings on the HashRing are remapped.
     */
    static int cacheRemapInterval;
    /**
     * Controls minimum interval (in milliseconds) between calls of CacheRedistributor.recordServerAngles() method.
     * Controls the temporal resolution of the HashRingAngle telemetry.
     */
    static int hashRingAngleRecordInterval;

    /**
     * Configures static variables
     * @param config: Config instance used for configuring variables on various classes
     */
    public static void configure( Config config ) {
        pingInterval = config.getCacheRedisPingInterval();
        cacheRemapInterval = config.getCacheRedisRemapInterval();
        hashRingAngleRecordInterval = config.getHashRingAngleRecordInterval();
    }

    /**
     * @param cacheRedis: Associated CacheRedistributor class which manages HashRing instance and handles consistent
     *                    hashing logic to delegate client requests to CacheServer instances
     */
    public CacheRedistributorRunnable( CacheRedistributor cacheRedis ) {
        this.cacheRedis = cacheRedis;
    }

    /**
     * Method for Runnable interface.
     * Prompts CacheRedistributor logic periodically
     */
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