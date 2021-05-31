package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.RequestDecoder;
import loadbalancerlab.shared.ServerInfo;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServerMonitorImpl implements ServerMonitor {
    ConcurrentMap<Integer, ServerInfo> serverInfoTable;
    HttpClientFactory clientFactory;
    RequestDecoder reqDecoder;
    SortedMap<Integer, Integer> serverCount;
    CacheServerManager cacheServerManager;
    boolean stopExecution;

    public ServerMonitorImpl( HttpClientFactory _clientFactory, RequestDecoder _reqDecoder, CacheServerManager _cacheServerManager ) {
        serverInfoTable = new ConcurrentHashMap<>();
        clientFactory = _clientFactory;
        serverCount = new TreeMap<>();
        reqDecoder = _reqDecoder;
        cacheServerManager = _cacheServerManager;
        stopExecution = false;
    }

    // ServerMonitor Interface
    // adds new cache server to record of servers
    @Override
    public void addServer( int id, int port ) {
        if (serverInfoTable.containsKey(id)) {
            throw new IllegalArgumentException("serverInfoTable already contains an entry for id " + id);
        }
        serverInfoTable.put(id, new ServerInfo(id, port));
    }

    @Override
    public void removeServer(int id) {
        if (serverInfoTable.containsKey(id)) {
            serverInfoTable.remove(id);
        }
    }

    // updates record of active number at a particular second in time
    @Override
    public void updateServerCount( int currentSecond, int numServers ) {
        if (!serverCount.containsKey(currentSecond))
            serverCount.put(currentSecond, numServers);
    }

    // outputs data about number of active servers vs. time
    @Override
    public SortedMap<Integer, Integer> deliverData() {
        SortedMap<Integer, Integer> copyMap = new TreeMap<>();
        copyMap.putAll(serverCount);
        return copyMap;
    }

    // returns server info table
    @Override
    public Map<Integer, ServerInfo> getServerInfo() {
        Map<Integer, ServerInfo> copyMap = new HashMap<>();
        copyMap.putAll(serverInfoTable);
        return copyMap;
    }

    @Override
    public void pingCacheServers() {
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
        List<Integer> ports = new ArrayList<>(this.serverInfoTable.keySet());

        for (Map.Entry<Integer, ServerInfo> entry : this.serverInfoTable.entrySet()) {
            ServerInfo info = entry.getValue();
            HttpGet req = new HttpGet("http://127.0.0.1:" + info.port + "/capacity-factor");

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
}