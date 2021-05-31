package loadbalancerlab.cacheservermanager;

public class CacheServerManagerRunnable implements Runnable {
    CacheServerManager cacheServerManager;
    ServerMonitorImpl serverMonitor;
    CacheInfoRequestHandler cacheInfoRequestHandler;

    CacheServerManagerRunnable cacheServerManagerRunnable;
    ServerMonitorRunnable serverMonitorRunnable;
    CacheInfoServerRunnable cacheInfoServerRunnable;

    Thread serverMonitorThread;
    Thread cacheInfoServerThread;
    int sleepInterval;

    public CacheServerManagerRunnable(CacheServerManagerConfig config) {
        // read from config
        cacheServerManager = config.cacheServerManager;
        serverMonitor = config.serverMonitor;
        serverMonitorRunnable = config.serverMonitorRunnable;
        cacheInfoRequestHandler = config.cacheInfoRequestHandler;
        cacheInfoServerRunnable = config.cacheInfoServerRunnable;
        serverMonitorThread = config.serverMonitorThread;
        cacheInfoServerThread = config.cacheInfoServerThread;
        sleepInterval = config.sleepInterval;
    }

    @Override
    public void run() {
        // start threads
        serverMonitorThread.start();
        cacheInfoServerThread.start();

        while (true) {
            try {
                cacheServerManager.modulateCapacity();
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                break;
            }
        }

        // interrupt sub-threads
        serverMonitorThread.interrupt();
        cacheInfoServerThread.interrupt();

        Thread.currentThread().interrupt();
    }
}