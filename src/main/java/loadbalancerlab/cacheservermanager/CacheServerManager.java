package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactoryImpl;
import loadbalancerlab.services.monitor.RequestMonitor;
import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.RequestDecoder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheServerManager implements Runnable {
    public static final int DEFAULT_PORT = 8000;
    static final int TICK_INTERVAL = 100;
    private int port;
    ConcurrentMap<Integer, Thread> serverThreadTable = new ConcurrentHashMap<>();
    private int[] selectablePorts = new int[100];
    private CacheServerFactoryImpl cacheServerFactory;
    private HttpClientFactory clientFactory;
    public RequestDecoder reqDecoder;
    ServerMonitor serverMonitor;
    static int cacheServerIdCounter;
    CacheInfoRequestHandler cacheInfoRequestHandler;
    CacheInfoServerRunnable cacheInfoServer;
    Runnable serverMonitorRunnable;
    Thread serverMonitorThread;
    Thread cacheInfoServerThread;

    static {
        cacheServerIdCounter = 0;
    }

    public CacheServerManager( CacheServerFactoryImpl _cacheServerFactory, HttpClientFactory _clientFactory, RequestDecoder _reqDecoder ) {
        port = -1;
        cacheServerFactory = _cacheServerFactory;
        clientFactory = _clientFactory;
        reqDecoder = _reqDecoder;

        // reserve ports 37000 through 37099 as usable ports
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;
        serverMonitor = new ServerMonitor(clientFactory, reqDecoder, this);
        cacheInfoRequestHandler = new CacheInfoRequestHandler(serverMonitor);
    }

    @Override
    public void run() {
        cacheInfoServer = new CacheInfoServerRunnable(DEFAULT_PORT, cacheInfoRequestHandler);
        serverMonitorRunnable = new ServerMonitorRunnable(serverMonitor, this);
        serverMonitorThread = new Thread(serverMonitorRunnable);
        cacheInfoServerThread = new Thread(cacheInfoServer);
        serverMonitorThread.start();
        cacheInfoServerThread.start();

        while (true) {
            try {
                Thread.sleep(TICK_INTERVAL);
            } catch (InterruptedException e) {
                Logger.log("CacheServerManager | CacheServerManager thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
            } finally {
                break;
            }
        }

        // shut down server monitor
        serverMonitorThread.interrupt();

        // shut down cache server threads
        for (Map.Entry<Integer, Thread> entry : serverThreadTable.entrySet()) {
            entry.getValue().interrupt();
            Logger.log("CacheServerManager | Terminating server id = " + entry.getKey(), Logger.LogType.THREAD_MANAGEMENT);
        }

        // shut down cache info server
        cacheInfoServerThread.interrupt();

        Logger.log("CacheServerManager | Terminated CacheServerManager thread", Logger.LogType.THREAD_MANAGEMENT);
        Thread.currentThread().interrupt();
    }

    public int getPort() {
        return this.port;
    }

    public SortedMap<Integer, Integer> deliverData() {
        return this.serverMonitor.deliverData();
    }

    public void startupCacheServer(int num) {
        for (int i = 0; i < num; i++) {
            CacheServer cacheServer = cacheServerFactory.produceCacheServer(new RequestMonitor("CacheServer"));
            Thread cacheServerThread = cacheServerFactory.produceCacheServerThread(cacheServer);
            cacheServerThread.start();
            serverThreadTable.put(cacheServerIdCounter, cacheServerThread);
            serverMonitor.addServer(cacheServerIdCounter, cacheServer.getPort());
            cacheServerIdCounter++;
        }
    }

    public void modulateCapacity() {

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

    int numServers() {
        return this.serverThreadTable.size();
    }
}
