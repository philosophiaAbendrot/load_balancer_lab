package loadbalancerlab.cacheservermanager;

import loadbalancerlab.shared.Logger;

/**
 * A Runnable implementation which wraps around ServerMonitor class
 */
public class ServerMonitorRunnable implements Runnable {
    /**
     * Associated ServerMonitor instance. Used for recording information on CacheServer instances.
     */
    ServerMonitor serverMonitor;
    /**
     * a flag which is used to control termination of loop in ServerMonitor run() method
     */
    boolean stopExecution;
    /**
     * Associated CacheServerManager. Used to manage lifecycle of CacheServer instances. Also modulates number of
     * CacheServer instances to meet request load.
     */
    CacheServerManager cacheServerManager;
    /**
     * minimum interval (in seconds) between calls of ServerMonitor.updateServerCount() method.
     */
    static int serverCountInterval = 1;
    /**
     * Stores timestamp of last call of ServerMonitor.updateServerCount() method.
     */
    int lastServerCountTime;
    /**
     * minimum interval (in seconds) between calls of ServerMonitor.pingCacheServers() method.
     */
    static int pingServerInterval = 1;
    /**
     * Stores timestamp of last call of ServerMonitor.pingCacheServers() method.
     */
    int lastPingServerTime;
    /**
     * Used for logging
     */
    private Logger logger;

    public ServerMonitorRunnable( ServerMonitor _serverMonitor, CacheServerManager _cacheServerManager) {
        serverMonitor = _serverMonitor;
        cacheServerManager = _cacheServerManager;
        stopExecution = false;
        logger = new Logger("ServerMonitorRunnable");
    }

    // Runnable Interface
    @Override
    public void run() {
        int currentTime = (int)(System.currentTimeMillis() / 1_000);
        lastPingServerTime = lastServerCountTime = currentTime;


        logger.log("Starting ServerMonitor", Logger.LogType.THREAD_MANAGEMENT);

        while (!this.stopExecution) {
            tick();
        }
    }

    // end of Runnable Interface
    void tick() {
        try {
            Thread.sleep(100);
            int currentSecond = (int)(System.currentTimeMillis() / 1_000);

            // updates record of active number of servers for a particular second
            if (currentSecond - lastServerCountTime >= serverCountInterval)
                serverMonitor.updateServerCount(currentSecond, cacheServerManager.numServers());

            // server monitor pings cache servers for checkup
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