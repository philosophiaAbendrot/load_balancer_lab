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

public class CacheRedistributorImpl implements CacheRedistributor {
    Map<Integer, ServerInfo> serverInfoTable;
    HashRing hashRing;

    static double targetCapacityFactor;
    private static RequestDecoder reqDecoder;
    private int cacheInfoServerPort;
    private static HttpClientFactory clientFactory;
    private static double[] serverLoadCutoffs;

    public static void configure( Config config ) {
        targetCapacityFactor = config.getTargetCapacityFactor();
        reqDecoder = config.getRequestDecoder();
        clientFactory = config.getClientFactory();
        serverLoadCutoffs = config.getServerLoadCutoffs();
    }

    public CacheRedistributorImpl(int _cacheServerManagerPort, HashRing _hashRing) {
        serverInfoTable = new HashMap<>();
        cacheInfoServerPort = _cacheServerManagerPort;
        hashRing = _hashRing;
    }

    // sends request to cache server manager for an update on which cache servers are running on which ports and
    // their capacity factors
    // updates the serverInfoTable field using the results
    @Override
    public void requestServerInfo() {
        CloseableHttpClient client = clientFactory.buildApacheClient();
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + cacheInfoServerPort + "/cache-servers");

        try {
            CloseableHttpResponse res = client.execute(getReq);
            JSONObject resJson = reqDecoder.extractJsonApacheResponse(res);
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
                    ServerInfo newInfo = new ServerInfoImpl(serverIdInt, port, cf);
                    serverInfoTable.put(serverIdInt, newInfo);
                }
            }
        } catch (IOException e) {
            Logger.log("CacheRedistributorImpl | Failed to send request to cache info server", Logger.LogType.REQUEST_PASSING);
        }
    }

    // returns the port of the cache server which is responsible for the resource
    // params:
    // resourceName: the name of the resource

    @Override
    public int selectPort( String resourceName ) throws IllegalStateException {
        int serverId = hashRing.findServerId(resourceName);

        if (!serverInfoTable.containsKey(serverId))
            throw new IllegalStateException("There is no corresponding server for this resource name");

        return serverInfoTable.get(serverId).getPort();
    }

    // remaps caching responsibility based on the load on each server
    @Override
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