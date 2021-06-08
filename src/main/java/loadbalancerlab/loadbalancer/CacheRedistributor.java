package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CacheRedistributor {
    Map<Integer, ServerInfo> serverInfoTable;
    HashRing hashRing;

    private static RequestDecoder reqDecoder;
    private int cacheServerManagerPort;
    private static HttpClientFactory httpClientFactory;
    private static double[] serverLoadCutoffs;

    public static void configure( Config config ) {
        reqDecoder = config.getRequestDecoder();
        httpClientFactory = config.getHttpClientFactory();
        serverLoadCutoffs = config.getServerLoadCutoffs();
    }

    public CacheRedistributor( int _cacheServerManagerPort, HashRing _hashRing) {
        serverInfoTable = new HashMap<>();
        cacheServerManagerPort = _cacheServerManagerPort;
        hashRing = _hashRing;
    }

    // sends request to cache server manager for an update on which cache servers are running on which ports and
    // their capacity factors
    // updates the serverInfoTable field using the results
    public void requestServerInfo() {
        System.out.println("CacheRedistributor | request server info running");
        CloseableHttpClient client = httpClientFactory.buildApacheClient();
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + cacheServerManagerPort + "/cache-servers");

        try {
            System.out.println("CacheRedistributor | attempting to send request to CacheServerManager");
            System.out.println("CacheRedistributor | ping request uri = " + getReq.getURI().toString());
            CloseableHttpResponse res = client.execute(getReq);
            JSONObject resJson = reqDecoder.extractJsonApacheResponse(res);
            System.out.println("resJson.keySet() = " + resJson.keySet());
            for (String serverId : resJson.keySet()) {
                int serverIdInt = Integer.valueOf(serverId);
                JSONObject entry = resJson.getJSONObject(serverId);
                double cf = entry.getDouble("capacityFactor");

                if (serverInfoTable.containsKey(serverIdInt)) {
                    // if serverInfoTable contains entry for this server
                    // update cf
                    serverInfoTable.get(serverIdInt).setCapacityFactor(cf);
                } else {
                    // otherwise, create new entry
                    int port = entry.getInt("port");
                    ServerInfo newInfo = new ServerInfo(serverIdInt, port, cf);
                    serverInfoTable.put(serverIdInt, newInfo);
                    // add server to HashRing
                    hashRing.addServer(serverIdInt);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log("CacheRedistributorImpl | Failed to send request to cache info server", Logger.LogType.REQUEST_PASSING);
        }
    }

    // returns the port of the cache server which is responsible for the resource
    // params:
    // resourceName: the name of the resource

    public int selectPort( String resourceName ) throws IllegalStateException {
        System.out.println("CacheRedistributor | selectPort called with resourceName = " + resourceName);
        int serverId = hashRing.findServerId(resourceName);
        System.out.println("CacheRedistributor | hashRing.findServerId returned id = " + serverId);

        if (!serverInfoTable.containsKey(serverId))
            throw new IllegalStateException("There is no corresponding server for this resource name");

        return serverInfoTable.get(serverId).getPort();
    }

    // remaps caching responsibility based on the load on each server
    public void remapCacheKeys() {
        for (Map.Entry<Integer, ServerInfo> entry : serverInfoTable.entrySet()) {
            int serverId = entry.getKey();
            ServerInfo info = entry.getValue();

            if (info.getCapacityFactor() < serverLoadCutoffs[1]) {
                // cf is lower than target range
                if (info.getCapacityFactor() < serverLoadCutoffs[0]) {
                    hashRing.addAngle(serverId, 3);
                } else {
                    hashRing.addAngle(serverId, 1);
                }
            } else if (info.getCapacityFactor() > serverLoadCutoffs[2]) {
                // cf is higher than target range
                if (info.getCapacityFactor() > serverLoadCutoffs[3]) {
                    hashRing.removeAngle(serverId, 3);
                } else {
                    hashRing.removeAngle(serverId, 1);
                }
            }
        }
    }
}