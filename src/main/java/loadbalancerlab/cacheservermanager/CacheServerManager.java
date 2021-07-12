package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.cacheserver.RequestMonitor;
import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.shared.Config;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.RequestDecoder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the lifecycle of CacheServer instances.
 * Modulates the number of CacheServers to match the request load.
 */
public class CacheServerManager {

    /**
     * Controls the speed of the growth or shrinking in the number of active CacheServer objects in response to the
     * request load.
     */
    static double growthRate;

    /**
     * The average capacity factor for CacheServers which the CacheServerManager aims to meet.
     */
    static double targetCf;

    /**
     * A concurrent hash map which maps cache servers by id to the threads that they are running on.
     */
    volatile ConcurrentMap<Integer, Thread> serverThreadTable = new ConcurrentHashMap<>();

    /**
     * The Associated ServerMonitor instance which records and updates info on the CacheServer instances.
     */
    ServerMonitor serverMonitor;

    /**
     * Static variable which is used to determine the id of the next CacheServer to be started.
     */
    static int cacheServerIdCounter;

    /**
     * Implementation of HttpRequestHandler interface which is used to handle requests for updates on a CacheServer.
     * instance's capacity factor.
     */
    CacheInfoRequestHandler cacheInfoRequestHandler;

    /**
     * Factory class used to create CacheServer instances.
     */
    private CacheServerFactory cacheServerFactory;

    /**
     * Factory class used to create CloseableHttpClient instances.
     */
    private HttpClientFactory clientFactory;

    /**
     * Utility class used to extract json fields from a CloseableHttpResponse instance.
     */
    private RequestDecoder reqDecoder;

    /**
     * The port that the CacheServerManager instance is running on.
     */
    private int port;

    static {
        cacheServerIdCounter = 0;
    }

    /**
     * Used to configure static variables on class.
     * @param config    Config class instance used to store configurations for various classes.
     */
    public static void configure( Config config ) {
        targetCf = config.getTargetCf();
        growthRate = config.getCacheServerGrowthRate();
    }

    /**
     * Constructor
     * @param cacheServerFactory    a factory class used to generate CacheServer instances.
     * @param clientFactory         a factory class used to generate CloseableHttpClient instances for sending requests.
     * @param reqDecoder            a utility class used to parse json from http responses.
     */
    public CacheServerManager( CacheServerFactory cacheServerFactory, HttpClientFactory clientFactory, RequestDecoder reqDecoder ) {
        port = -1;
        this.cacheServerFactory = cacheServerFactory;
        this.clientFactory = clientFactory;
        this.reqDecoder = reqDecoder;

        /* Server monitor is set */
        serverMonitor = new ServerMonitor(clientFactory, reqDecoder, this);
        cacheInfoRequestHandler = new CacheInfoRequestHandler(serverMonitor);
    }

    /**
     * Getter method for the port that this object is running on.
     * @return  The port that this object is running on.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Getter method for 'serverCount' field on ServerMonitor class
     *
     * @return  Maps timestamps (seconds since 1-Jan-1970) to the number of CacheServers active by time.
     *          The data object is a copy of the 'serverCount' field on ServerMonitor class.
     */
    public SortedMap<Integer, Integer> deliverServerCountData() {
        return this.serverMonitor.deliverServerCountData();
    }

    /**
     * Generates CacheServer instances, allocates it a thread on which to run, and starts the thread.
     * Updates 'serverThreadTable' field, which keeps track of cache server ids and the threads the CacheServer
     * instances are running on.
     * @param num   parameter which controls how many CacheServer instances are being generated and started.
     */
    public void startupCacheServer(int num) {
        for (int i = 0; i < num; i++) {
            CacheServer cacheServer = cacheServerFactory.produceCacheServer(new RequestMonitor());
            Thread cacheServerThread = cacheServerFactory.produceCacheServerThread(cacheServer);
            cacheServerThread.start();
            serverThreadTable.put(cacheServerIdCounter, cacheServerThread);

            /* Wait for cache server to startup and set its port */
            while (cacheServer.getPort() == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            int currentTime = (int)(System.currentTimeMillis() / 1_000);
            serverMonitor.addServer(cacheServerIdCounter, cacheServer.getPort(), currentTime);
            cacheServerIdCounter++;
        }
    }

    /**
     * Modulates number of active CacheServer instances by using the average capacity factor of all CacheServer
     * instances.
     */
    public void modulateCapacity() {
        double averageCapacityFactor = serverMonitor.getAverageCf();
        double diff = averageCapacityFactor - targetCf;
        int intDiff = (int)(Math.round(diff * (growthRate / 100) * serverThreadTable.size()));

        if (diff == 0) {
            return;
        } else if (diff > 0) {
            startupCacheServer(intDiff);
        } else {
            shutdownCacheServer(-intDiff);
        }
    }

    /**
     * Shuts down CacheServer threads and removes the terminated thread from 'serverThreadTable'.
     * Updates records in ServerMonitor instance to reflect that CacheServer instances have been terminated.
     * @param num   The number of CacheServers to be deactivated.
     */
    public void shutdownCacheServer(int num) {
        List<Integer> serverIds = new ArrayList<>(serverThreadTable.keySet());
        Random rand = new Random();
        num = Math.min(serverThreadTable.size(), num);

        for (int i = 0; i < num; i++) {
            int randIdx = rand.nextInt(serverIds.size());
            int selectedId = serverIds.get(randIdx);
            Thread selectedThread = serverThreadTable.get(selectedId);
            selectedThread.interrupt();
            serverThreadTable.remove(selectedId);
            serverMonitor.deactivateServer(selectedId, (int)(System.currentTimeMillis() / 1_000));
            serverIds.remove(randIdx);
        }
    }

    /**
     * Getter method for the number of CacheServer instances which are currently active.
     * @return      the number of CacheServer instances which are currently active.
     */
    public int numServers() {
        return this.serverThreadTable.size();
    }

    /**
     * @return: Returns capacity factors of CacheServer instances as a function of time.
     */
    public String[][] deliverCfData() {
        return serverMonitor.deliverCfData();
    }
}
