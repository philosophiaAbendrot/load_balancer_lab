package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;

public class CacheServerManagerRunnable implements Runnable {
    CacheServerManager cacheServerManager;
    ServerMonitor serverMonitor;
    CacheInfoRequestHandler cacheInfoRequestHandler;

    ServerMonitorRunnable serverMonitorRunnable;
    CacheInfoServerRunnable cacheInfoServerRunnable;

    Thread serverMonitorThread;
    Thread cacheInfoServerThread;
    static int sleepInterval = 50;

    RequestDecoder reqDecoder;
    CacheServerFactory cacheServerFactory;
    HttpClientFactory clientFactory;

    static int numCacheServersOnStartup;
    static int capacityModulationInterval;
    private int lastCapacityModulationTime;

    public CacheServerManagerRunnable( CacheServerFactory _cacheServerFactory, HttpClientFactory _clientFactory, RequestDecoder _reqDecoder, CacheServerManager _cacheServerManager ) {
        reqDecoder = _reqDecoder;
        clientFactory = _clientFactory;
        cacheServerFactory = _cacheServerFactory;

        cacheServerManager = _cacheServerManager;

        // generate instances of sub-components
        serverMonitor = cacheServerManager.serverMonitor;
        cacheInfoRequestHandler = new CacheInfoRequestHandler(serverMonitor);

        // generate runnables for sub-components
        serverMonitorRunnable = new ServerMonitorRunnable(serverMonitor, cacheServerManager);
        cacheInfoServerRunnable = new CacheInfoServerRunnable(cacheInfoRequestHandler);

        // generate threads for sub-components
        serverMonitorThread = new Thread(serverMonitorRunnable);
        cacheInfoServerThread = new Thread(cacheInfoServerRunnable);
    }

    public int getPort() {
        return cacheInfoServerRunnable.getPort();
    }

    public static void configure(Config config) {
        numCacheServersOnStartup = config.getNumCacheServersOnStartup();
        capacityModulationInterval = config.getCapacityModulationInterval();
    }

    @Override
    public void run() {
        // start sub-threads
        serverMonitorThread.start();
        cacheInfoServerThread.start();

        // startup cache servers
        cacheServerManager.startupCacheServer(numCacheServersOnStartup);

        int currentTime = (int)(System.currentTimeMillis() / 1_000);
        lastCapacityModulationTime = currentTime;

        while (true) {
            currentTime = (int)(System.currentTimeMillis() / 1_000);
            try {
                if (currentTime - lastCapacityModulationTime >= capacityModulationInterval) {
                    lastCapacityModulationTime = currentTime;
                    cacheServerManager.modulateCapacity();
                }
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                break;
            }
        }

        // interrupt sub-threads
        serverMonitorThread.interrupt();
        cacheInfoServerThread.interrupt();

        // interrupt cache server threads
        for (Thread serverThread : cacheServerManager.serverThreadTable.values()) {
            serverThread.interrupt();
        }
    }
}