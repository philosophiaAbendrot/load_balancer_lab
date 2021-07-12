package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;

/**
 * Runnable implementation which serves as a wrapper for CacheServerManager class.
 */
public class CacheServerManagerRunnable implements Runnable {

    /**
     * Associated CacheServerManager instance.
     * Manages lifecycle of CacheServer instances.
     */
    CacheServerManager cacheServerManager;

    /**
     * Associated ServerMonitor instance which is used to keep track of the data on all the CacheServer instances.
     */
    ServerMonitor serverMonitor;

    /**
     * A HttpRequestHandler implementation which is used to handle requests for updates on the capacity factor
     * of specific CacheServer instances.
     */
    CacheInfoRequestHandler cacheInfoRequestHandler;

    /**
     * An implementation of the Runnable interface which wraps around the associated ServerMonitor instance.
     * Used for starting a thread which runs ServerMonitor logic periodically.
     */
    ServerMonitorRunnable serverMonitorRunnable;

    /**
     * A server which handles requests for updates on CacheServer capacity factors.
     */
    CacheInfoServerRunnable cacheInfoServerRunnable;

    /**
     * Thread on which ServerMonitor instance runs.
     */
    Thread serverMonitorThread;

    /**
     * Thread on which CacheInfoServerRunnable instance runs.
     */
    Thread cacheInfoServerThread;

    /**
     * Controls how long the thread sleeps (in milliseconds) between iterations of the run() method.
     */
    static int sleepInterval = 50;

    /**
     * Utility class used to extract json fields from a CloseableHttpResponse instance.
     */
    RequestDecoder reqDecoder;

    /**
     * Factory class used to create CacheServer instances.
     */
    CacheServerFactory cacheServerFactory;

    /**
     * Factory class used to create CloseableHttpClient instances.
     */
    HttpClientFactory clientFactory;

    /**
     * Static variable which controls the number of CacheServers which are started initially.
     */
    static int numCacheServersOnStartup;

    /**
     * Static variable which controls the minimum time (in milliseconds) between calls of
     * CacheServerManager.modulateCapacity() method.
     */
    static int capacityModulationInterval;

    /**
     * Keeps track of the last timestamp (in seconds since 1-Jan-1970) at which CacheServerManager.modulateCapacity()
     * method was called.
     * Used in conjunction with 'capacityModulationInterval' field to get CacheServerManager.modulateCapacity() method
     * to be called periodically.
     */
    private int lastCapacityModulationTime;

    /**
     * Constructor
     * @param cacheServerFactory  Factory class used to generate CacheServer instances.
     * @param clientFactory       Factory class used to generate CloseableHttpClient instances.
     * @param reqDecoder          Utility class used to extract json parameters from a CloseableHttpResponse instance.
     * @param cacheServerManager  Object used to manage life cycle of CacheServer instances and modulate the number of
     *                            CacheServer instances to meet request load.
     */
    public CacheServerManagerRunnable( CacheServerFactory cacheServerFactory, HttpClientFactory clientFactory, RequestDecoder reqDecoder, CacheServerManager cacheServerManager ) {
        this.reqDecoder = reqDecoder;
        this.clientFactory = clientFactory;
        this.cacheServerFactory = cacheServerFactory;
        this.cacheServerManager = cacheServerManager;

        /* Generate instances of sub-components */
        serverMonitor = cacheServerManager.serverMonitor;
        cacheInfoRequestHandler = new CacheInfoRequestHandler(serverMonitor);

        /* Wrap sub-components in Runnable objects */
        serverMonitorRunnable = new ServerMonitorRunnable(serverMonitor, cacheServerManager);
        cacheInfoServerRunnable = new CacheInfoServerRunnable(cacheInfoRequestHandler);

        /* Generate threads for sub-components */
        serverMonitorThread = new Thread(serverMonitorRunnable);
        cacheInfoServerThread = new Thread(cacheInfoServerRunnable);
    }

    /**
     * Getter method for the port that the associated CacheInfoServerRunnable object is running on.
     * @return      The port that the associated CacheInfoServerRunnable is running on.
     */
    public int getPort() {
        return cacheInfoServerRunnable.getPort();
    }

    /**
     * Method used to configure static variables.
     * @param config     Method used to configure static variables.
     */
    public static void configure(Config config) {
        numCacheServersOnStartup = config.getNumCacheServersOnStartup();
        capacityModulationInterval = config.getCapacityModulationInterval();
    }

    /**
     * Starts sub-threads for ServerMonitor instance and CacheInfoServer instance.
     * Starts default number of cache server instances.
     * Method from Runnable interface/
     * Runs CacheServerManager.modulateCapacity() method periodically to modulate the number of CacheServers to meet
     * request load.
     * When interrupted, shuts down the associated sub-threads, including the threads that the CacheServer instances
     * run on.
     */
    @Override
    public void run() {

        /* Start sub-threads */
        serverMonitorThread.start();
        cacheInfoServerThread.start();

        /* Startup cache servers */
        cacheServerManager.startupCacheServer(numCacheServersOnStartup);

        int currentTime = (int)(System.currentTimeMillis() / 1_000);
        lastCapacityModulationTime = currentTime;

        while (true) {
            currentTime = (int)(System.currentTimeMillis() / 1_000);
            try {

                /* Run 'modulateCapacity' method on CacheServerManager if it has been long enough since the last call
                 */
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

        /* Interrupt sub-threads */
        serverMonitorThread.interrupt();
        cacheInfoServerThread.interrupt();

        /* Interrupt cache server threads */
        for (Thread serverThread : cacheServerManager.serverThreadTable.values()) {
            serverThread.interrupt();
        }
    }
}