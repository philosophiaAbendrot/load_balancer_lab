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
        System.out.println("startupCacheServer called");
        for (int i = 0; i < num; i++) {
            System.out.println("path o");
            CacheServer cacheServer = cacheServerFactory.produceCacheServer(new RequestMonitor());
            System.out.println("path p");
            Thread cacheServerThread = cacheServerFactory.produceCacheServerThread(cacheServer);
            System.out.println("path q");
            cacheServerThread.start();
            System.out.println("path r");
            serverThreadTable.put(cacheServerIdCounter, cacheServerThread);
            System.out.println("path s");

            // wait for cache server to startup and set its port
            while (cacheServer.getPort() == 0) {
                System.out.println("path t");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("path u");

            serverMonitor.addServer(cacheServerIdCounter, cacheServer.getPort());
            System.out.println("path v");
            cacheServerIdCounter++;
            System.out.println("path w");
        }
    }

    public void modulateCapacity() {
        System.out.println("path a");
        double averageCapacityFactor = serverMonitor.getAverageCf();
        System.out.println("path b");
        double diff = averageCapacityFactor - targetCf;
        System.out.println("path c");
        int intDiff = (int)(Math.round(diff * (growthRate / 100) * serverThreadTable.size()));
        System.out.println("path d");

        if (diff == 0) {
            System.out.println("path e");
            return;
        } else if (diff > 0) {
            System.out.println("path f");
            System.out.println("intDiff = " + intDiff);
            startupCacheServer(intDiff);
        } else {
            System.out.println("path g");
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
