package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;

/**
 * Runnable implementation which serves as a wrapper for CacheServerManager class
 */
public class CacheServerManagerRunnable implements Runnable {
    /**
     * Associated CacheServerManager instance
     */
    CacheServerManager cacheServerManager;
    /**
     * Associated ServerMonitor instance which is used to keep track of the data on all the CacheServer instances
     */
    ServerMonitor serverMonitor;
    /**
     * A HttpRequestHandler implementation which is used to handle requests for updates on the capacity factor
     * of specific CacheServer instances
     */
    CacheInfoRequestHandler cacheInfoRequestHandler;
    /**
     * An implementation of the Runnable interface which wraps around the associated ServerMonitor instance
     * Used for starting a thread which runs ServerMonitor logic periodically
     */
    ServerMonitorRunnable serverMonitorRunnable;
    /**
     * A server which handles requests for data on CacheServer capacity factors
     */
    CacheInfoServerRunnable cacheInfoServerRunnable;
    /**
     * Thread on which ServerMonitor instance runs
     */
    Thread serverMonitorThread;
    /**
     * Thread on which CacheInfoServerRunnable instance runs
     */
    Thread cacheInfoServerThread;
    /**
     * controls how long the thread sleeps (in milliseconds) between iterations of the run() method
     */
    static int sleepInterval = 50;
    /**
     * Utility class used to extract json fields from a CloseableHttpResponse instance
     */
    RequestDecoder reqDecoder;
    /**
     * Factory class used to create CacheServer instances
     */
    CacheServerFactory cacheServerFactory;
    /**
     * Factory class used to create CloseableHttpClient instances
     */
    HttpClientFactory clientFactory;
    /**
     * Static variable which controls the number of CacheServers which are started initially
     */
    static int numCacheServersOnStartup;
    /**
     * Static variable which controls the minimum time (in milliseconds) between calls of CacheServerManager.modulateCapacity() method.
     */
    static int capacityModulationInterval;
    /**
     * keeps track of the last timestamp (in seconds since 1-Jan-1970) at which CacheServerManager.modulateCapacity() method was called.
     * Used in conjunction with 'capacityModulationInterval' field to get CacheServerManager.modulateCapacity() method
     * to be called periodically.
     */
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