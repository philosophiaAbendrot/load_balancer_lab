package loadbalancerlab.cacheservermanager;

import loadbalancerlab.shared.Logger;

/**
 * A Runnable implementation which wraps around a ServerMonitor object.
 */
public class ServerMonitorRunnable implements Runnable {

    /**
     * Associated ServerMonitor instance. Used for recording information on CacheServer instances.
     */
    ServerMonitor serverMonitor;

    /**
     * A flag which is used to control termination of loop in ServerMonitor run() method.
     */
    boolean stopExecution;

    /**
     * Associated CacheServerManager. Used to manage lifecycle of CacheServer instances.
     * Also modulates number of CacheServer instances to meet request load.
     */
    CacheServerManager cacheServerManager;

    /**
     * Minimum interval (in seconds) between calls of ServerMonitor.updateServerCount() method.
     */
    static int serverCountInterval = 1;

    /**
     * Stores timestamp of last call of ServerMonitor.updateServerCount() method.
     */
    int lastServerCountTime;

    /**
     * Minimum interval (in seconds) between calls of ServerMonitor.pingCacheServers() method.
     */
    static int pingServerInterval = 1;

    /**
     * Stores timestamp of last call of ServerMonitor.pingCacheServers() method.
     */
    int lastPingServerTime;

    /**
     * Object used for logging.
     */
    private Logger logger;

    /**
     * Constructor
     * @param serverMonitor         Associated ServerMonitor instance.
     * @param cacheServerManager    Associated CacheServerManager instance.
     */
    public ServerMonitorRunnable( ServerMonitor serverMonitor, CacheServerManager cacheServerManager ) {
        this.serverMonitor = serverMonitor;
        this.cacheServerManager = cacheServerManager;
        stopExecution = false;
        logger = new Logger("ServerMonitorRunnable");
    }

    /**
     * Method from Runnable interface.
     * Periodically runs 'updateServerCount()' and 'pingCacheServers()' methods on the associated
     * ServerMonitor instance
     */
    @Override
    public void run() {
        int currentTime = (int)(System.currentTimeMillis() / 1_000);
        lastPingServerTime = lastServerCountTime = currentTime;

        logger.log("Starting ServerMonitor", Logger.LogType.THREAD_MANAGEMENT);

        while (!this.stopExecution) {
            try {
                Thread.sleep(100);
                int currentSecond = (int)(System.currentTimeMillis() / 1_000);

                /* Updates record of active number of servers for a particular second */
                if (currentSecond - lastServerCountTime >= serverCountInterval)
                    serverMonitor.updateServerCount(currentSecond, cacheServerManager.numServers());

                /* Server monitor pings cache servers for checkup */
                if (currentSecond - lastPingServerTime >= pingServerInterval)
                    serverMonitor.pingCacheServers();

            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                logger.log("Shutting down ServerMonitorRunnable", Logger.LogType.THREAD_MANAGEMENT);
                this.stopExecution = true;
            }
        }
    }
}