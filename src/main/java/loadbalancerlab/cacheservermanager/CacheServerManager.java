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

public class CacheServerManager {
    static double growthRate;
    static double targetCf;
    volatile ConcurrentMap<Integer, Thread> serverThreadTable = new ConcurrentHashMap<>();
    ServerMonitor serverMonitor;
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
        System.out.println("CacheServerManager initializer called");
        port = -1;
        this.cacheServerFactory = cacheServerFactory;
        this.clientFactory = clientFactory;
        this.reqDecoder = reqDecoder;

        // reserve ports 37000 through 37099 as usable ports
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;

        serverMonitor = new ServerMonitor(clientFactory, reqDecoder, this);
        cacheInfoRequestHandler = new CacheInfoRequestHandler(serverMonitor);
    }

    public int getPort() {
        return this.port;
    }

    public SortedMap<Integer, Integer> deliverData() {
        return this.serverMonitor.deliverData();
    }

    public void startupCacheServer(int num) {
        System.out.println("CacheServerManager | startupCacheServer running | num = " + num);
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

            serverMonitor.addServer(cacheServerIdCounter, cacheServer.getPort());
            cacheServerIdCounter++;
            System.out.println("CacheServerManager | adding cacheServer i = " + i);
        }
    }

    public void modulateCapacity() {
        System.out.println("CacheServerManager | modulateCapacity called");
        double averageCapacityFactor = serverMonitor.getAverageCf();
        System.out.println("CacheServerManager | averageCapacityFactor = " + averageCapacityFactor);
        double diff = averageCapacityFactor - targetCf;
        int intDiff = (int)(Math.round(diff * growthRate));

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
            serverMonitor.removeServer(selectedId);
            serverIds.remove(randIdx);
        }
    }

    public int numServers() {
        return this.serverThreadTable.size();
    }
}
