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
 * A server which manages the lifecycle of CacheServer instances
 * Modulates the number of CacheServers to match the request load
 */
public class CacheServerManager {
    /**
     * used to control the rate at which servers are spawned or shutdown
     */
    static double growthRate;
    /**
     * The average capacity factor for CacheServers which the CacheServerManager aims to meet
     */
    static double targetCf;
    /**
     * A concurrent hash map which maps cache servers by id to the threads that they are running on
     */
    volatile ConcurrentMap<Integer, Thread> serverThreadTable = new ConcurrentHashMap<>();
    /**
     * The Associated ServerMonitor instance which records and updates info on the CacheServer instances
     */
    ServerMonitor serverMonitor;
    /**
     * Static variable which is used to determine the id of the next CacheServer to be started
     */
    static int cacheServerIdCounter;
    int[] selectablePorts = new int[100];
    CacheInfoRequestHandler cacheInfoRequestHandler;

    private CacheServerFactory cacheServerFactory;
    private HttpClientFactory clientFactory;
    private RequestDecoder reqDecoder;
    private int port;


    static {
        cacheServerIdCounter = 0;
    }

    public static void configure( Config config ) {
        targetCf = config.getTargetCf();
        growthRate = config.getCacheServerGrowthRate();
    }

    public CacheServerManager( CacheServerFactory cacheServerFactory, HttpClientFactory clientFactory, RequestDecoder reqDecoder ) {
        port = -1;
        this.cacheServerFactory = cacheServerFactory;
        this.clientFactory = clientFactory;
        this.reqDecoder = reqDecoder;

        // reserve ports 37000 through 37099 as usable ports
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;

        // server monitor is set
        serverMonitor = new ServerMonitor(clientFactory, reqDecoder, this);
        cacheInfoRequestHandler = new CacheInfoRequestHandler(serverMonitor);
    }

    public int getPort() {
        return this.port;
    }

    public SortedMap<Integer, Integer> deliverServerCountData() {
        return this.serverMonitor.deliverServerCountData();
    }

    public void startupCacheServer(int num) {
        for (int i = 0; i < num; i++) {
            CacheServer cacheServer = cacheServerFactory.produceCacheServer(new RequestMonitor());
            Thread cacheServerThread = cacheServerFactory.produceCacheServerThread(cacheServer);
            cacheServerThread.start();
            serverThreadTable.put(cacheServerIdCounter, cacheServerThread);

            // wait for cache server to startup and set its port
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

    public int numServers() {
        return this.serverThreadTable.size();
    }

    public String[][] deliverCfData() {
        return serverMonitor.deliverCfData();
    }
}
