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
    /**
     * A hash table which maps server id to a ServerInfo instance which holds info about the server
     * holds info about the port and the record of capacity factor over time
     */
    ConcurrentMap<Integer, ServerInfo> serverInfoTable;
    /**
     * A factory which is used to generate CloseableHttpClient instances
     */
    HttpClientFactory clientFactory;
    /**
     * Used to extract information from CloseableHttpResponse objects
     */
    RequestDecoder reqDecoder;
    /**
     * Used to store information about how many servers were active at each moment in time
     */
    SortedMap<Integer, Integer> serverCount;
    /**
     * Associated CacheServerManager instance which manages lifecycle of cache servers and modulates number of
     * cache servers to meet request load
     */
    CacheServerManager cacheServerManager;
    /**
     * variable which is used to control when ServerMonitor thread stops execution
     */
    boolean stopExecution;

    public ServerMonitor( HttpClientFactory _clientFactory, RequestDecoder _reqDecoder, CacheServerManager _cacheServerManager ) {
        serverInfoTable = new ConcurrentHashMap<>();
        clientFactory = _clientFactory;
        serverCount = new TreeMap<>();
        reqDecoder = _reqDecoder;
        cacheServerManager = _cacheServerManager;
        stopExecution = false;
    }

    /**
     * Adds new cache server to the record of servers 'serverInfoTable' field
     * @param id: the id of the server
     * @param port: the port that the server is running on
     */
    public void addServer( int id, int port ) {
        if (serverInfoTable.containsKey(id)) {
            throw new IllegalArgumentException("serverInfoTable already contains an entry for id " + id);
        }
        serverInfoTable.put(id, new ServerInfo(id, port));
    }

    /**
     * Removes info about server from serverInfoTable field
     * @param id: the id of the server
     */
    public void removeServer(int id) {
        if (serverInfoTable.containsKey(id)) {
            serverInfoTable.remove(id);
        }
    }

    /**
     * Updates the number of active servers at a particular moment in time in 'serverCount' field
     * @param currentSecond: the current second, in seconds since 1-Jan-1970
     * @param numServers: the number of active servers at the time indicated by currentSecond
     */
    public void updateServerCount( int currentSecond, int numServers ) {
        if (!serverCount.containsKey(currentSecond))
            serverCount.put(currentSecond, numServers);
    }

    /**
     * @return info about the number of active cache servers as a function of time
     */
    public SortedMap<Integer, Integer> deliverServerCountData() {
        SortedMap<Integer, Integer> copyMap = new TreeMap<>();
        copyMap.putAll(serverCount);
        return copyMap;
    }

    /**
     * @return a copy of serverInfoTable
     */
    public Map<Integer, ServerInfo> getServerInfo() {
        Map<Integer, ServerInfo> copyMap = new HashMap<>();
        copyMap.putAll(serverInfoTable);
        return copyMap;
    }

    /**
     * Pings every active cache server and updates capacity factor information in serverInfoTable
     */
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

    /**
     * @return average capacity factor of every cache server as a double
     */
    // returns the average capacity factor of every cache server
    public double getAverageCf() {
        double cfSum = 0;
        int numEntries = 0;

        for (ServerInfo info : serverInfoTable.values()) {
            if (info.getCurrentCapacityFactor() != 0.0) {
                cfSum += info.getCurrentCapacityFactor();
                numEntries++;
            }
        }

        // return average
        return cfSum / numEntries;
    }

    /**
     * Returns data on the capacity factor of each server at each moment in time
     * @return returns data on cap factor at each moment in time in the format of a 2d string array, which is
     * well suited to conversion to csv format
     */
    public String[][] deliverCfData() {


        return new String[0][0];
    }
}