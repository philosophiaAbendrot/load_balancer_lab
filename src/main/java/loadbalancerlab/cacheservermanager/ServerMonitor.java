package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServerMonitor {
    ConcurrentMap<Integer, ServerInfo> serverInfoTable;
    HttpClientFactory clientFactory;
    RequestDecoder reqDecoder;
    SortedMap<Integer, Integer> serverCount;
    CacheServerManager cacheServerManager;
    boolean stopExecution;

    public ServerMonitor( HttpClientFactory _clientFactory, RequestDecoder _reqDecoder, CacheServerManager _cacheServerManager ) {
        serverInfoTable = new ConcurrentHashMap<>();
        clientFactory = _clientFactory;
        serverCount = new TreeMap<>();
        reqDecoder = _reqDecoder;
        cacheServerManager = _cacheServerManager;
        stopExecution = false;
    }

    // adds new cache server to record of servers
    // params: id: id of the server
    //         port: port that the server is running on
    public void addServer( int id, int port ) {
        if (serverInfoTable.containsKey(id)) {
            throw new IllegalArgumentException("serverInfoTable already contains an entry for id " + id);
        }
        serverInfoTable.put(id, new ServerInfo(id, port));
    }

    // removes server from record
    // params: id: id of the server
    public void removeServer(int id) {
        if (serverInfoTable.containsKey(id)) {
            serverInfoTable.remove(id);
        }
    }

    // updates record of active number at a particular second in time
    public void updateServerCount( int currentSecond, int numServers ) {
        if (!serverCount.containsKey(currentSecond))
            serverCount.put(currentSecond, numServers);
    }

    // outputs data about number of active servers vs. time
    public SortedMap<Integer, Integer> deliverServerCountData() {
        SortedMap<Integer, Integer> copyMap = new TreeMap<>();
        copyMap.putAll(serverCount);
        return copyMap;
    }

    // returns server info table
    public Map<Integer, ServerInfo> getServerInfo() {
        Map<Integer, ServerInfo> copyMap = new HashMap<>();
        copyMap.putAll(serverInfoTable);
        return copyMap;
    }

    // pings cache servers for updates on capacity factor and records the info
    public void pingCacheServers() {
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
        List<Integer> ports = new ArrayList<>(this.serverInfoTable.keySet());

        for (Map.Entry<Integer, ServerInfo> entry : this.serverInfoTable.entrySet()) {
            ServerInfo info = entry.getValue();
            HttpGet req = new HttpGet("http://127.0.0.1:" + info.getPort() + "/capacity-factor");

            try {
                CloseableHttpResponse response = httpClient.execute(req);
                JSONObject responseJson = this.reqDecoder.extractJsonApacheResponse(response);
                info.updateCapacityFactor((int) (System.currentTimeMillis() / 1_000), responseJson.getDouble("capacity_factor"));
            } catch (IOException e) {
                System.out.println("IOException thrown in ServerMonitorImpl#pingCacheServer");
                e.printStackTrace();
            }
        }
    }

    // returns the average capacity factor of every cache server
    public double getAverageCf() {
        double cfSum = 0;
        int numEntries = 0;

        for (ServerInfo info : serverInfoTable.values()) {
            if (info.getAverageCapacityFactor() != 0.0) {
                cfSum += info.getAverageCapacityFactor();
                numEntries++;
            }
        }

        // return average
        return cfSum / numEntries;
    }
}