package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoder;
import org.apache.http.HttpEntity;
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

    public static void configure( Config config ) {
        targetCapacityFactor = config.getTargetCapacityFactor();
        reqDecoder = config.getRequestDecoder();
        clientFactory = config.getClientFactory();
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

    @Override
    public int selectPort( String resourceName ) {
        return 0;
    }

    @Override
    public void remapCacheKeys() {

    }
}