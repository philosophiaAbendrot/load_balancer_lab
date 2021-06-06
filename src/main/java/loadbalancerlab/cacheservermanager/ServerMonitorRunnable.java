package loadbalancerlab.cacheservermanager;

import loadbalancerlab.shared.Logger;


public class ServerMonitorRunnable implements Runnable {
    ServerMonitor serverMonitor;
    boolean stopExecution;
    CacheServerManager cacheServerManager;
    static int serverCountInterval = 1;
    int lastServerCountTime;

    public ServerMonitorRunnable( ServerMonitor _serverMonitor, CacheServerManager _cacheServerManager) {
        serverMonitor = _serverMonitor;
        cacheServerManager = _cacheServerManager;
        stopExecution = false;
    }

    // Runnable Interface
    @Override
    public void run() {
        int currentTime = (int)(System.currentTimeMillis() / 1_000);
        lastServerCountTime = currentTime;

        Logger.log("ServerMonitorRunnable | Starting ServerMonitor", Logger.LogType.THREAD_MANAGEMENT);

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
            if (currentSecond - lastServerCountTime >= serverCountInterval) {
                serverMonitor.updateServerCount(currentSecond, cacheServerManager.numServers());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("ServerMonitorRunnable | Shutting down ServerMonitorRunnable", Logger.LogType.THREAD_MANAGEMENT);
            this.stopExecution = true;
        }
    }
}