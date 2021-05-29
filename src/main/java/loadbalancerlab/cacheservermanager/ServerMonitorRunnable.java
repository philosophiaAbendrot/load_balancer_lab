package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.util.RequestDecoder;
import loadbalancerlab.util.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServerMonitorRunnable implements ServerMonitor, Runnable {
    ConcurrentMap<Integer, ServerInfo> serverInfoTable;
    HttpClientFactory clientFactory;
    RequestDecoder reqDecoder;
    SortedMap<Integer, Integer> serverCount;
    CacheServerManager cacheServerManager;
    boolean stopExecution;

    static class ServerInfo {
        int id;
        int port;
        double capacityFactor;

        public ServerInfo( int id, int port ) {
            this.id = id;
            this.port = port;
        }
    }

    public ServerMonitorRunnable( HttpClientFactory httpClientFactory, RequestDecoder reqDecoder, CacheServerManager cacheServerManager ) {
        this.serverInfoTable = new ConcurrentHashMap<>();
        this.clientFactory = httpClientFactory;
        this.serverCount = new TreeMap<>();
        this.reqDecoder = reqDecoder;
        this.cacheServerManager = cacheServerManager;
        this.stopExecution = false;
    }

    @Override
    public void run() {
        Logger.log("ServerMonitorRunnable | Starting ServerMonitor", Logger.LogType.THREAD_MANAGEMENT);

        while (!this.stopExecution) {
            tick();
        }
    }

    void tick() {
        try {
            Thread.sleep(100);
            int currentSecond = (int)(System.currentTimeMillis() / 1_000);
            updateServerCount(currentSecond, cacheServerManager.numServers());
        } catch (InterruptedException e) {
            Logger.log("ServerMonitorRunnable | Shutting down ServerMonitorRunnable", Logger.LogType.THREAD_MANAGEMENT);
            Thread.currentThread().interrupt();
            this.stopExecution = true;
        }
    }

    @Override
    public void pingCacheServers( long currentTime ){
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
        List<Integer> ports = new ArrayList<>(this.serverInfoTable.keySet());

        for (Map.Entry<Integer, ServerInfo> entry : this.serverInfoTable.entrySet()) {
            int port = entry.getKey();
            ServerInfo info = entry.getValue();
            HttpGet req = new HttpGet("http://127.0.0.1:" + port + "/capacity-factor");

            try {
                CloseableHttpResponse response = httpClient.execute(req);
                JSONObject responseJson = this.reqDecoder.extractJsonApacheResponse(response);
                info.capacityFactor = responseJson.getDouble("capacity_factor");
            } catch (IOException e) {
                System.out.println("IOException thrown in ServerMonitorImpl#pingCacheServer");
                e.printStackTrace();
            }
        }
    }

    // adds new cache server to record of servers
    @Override
    public void addServer( int id, int port ) {
        if (this.serverInfoTable.containsKey(id)) {
            throw new IllegalArgumentException("serverInfoTable already contains an entry for id " + id);
        }
        this.serverInfoTable.put(id, new ServerInfo(id, port));
    }

    @Override
    public void updateServerCount( int currentSecond, int numServers ) {
        if (!this.serverCount.containsKey(currentSecond))
            this.serverCount.put(currentSecond, numServers);
    }

    @Override
    public SortedMap<Integer, Integer> deliverData() {
        SortedMap<Integer, Integer> copyMap = new TreeMap<>();
        copyMap.putAll(this.serverCount);
        return copyMap;
    }
}