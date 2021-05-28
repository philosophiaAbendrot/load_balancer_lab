package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.util.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServerMonitorImpl implements ServerMonitor {
    private static class ServerInfo {
        int id;
        int port;
        double capacityFactor;

        public ServerInfo( int id, int port ) {
            this.id = id;
            this.port = port;
        }
    }

    ConcurrentMap<Integer, ServerInfo> serverInfoTable;
    HttpClientFactory clientFactory;
    RequestDecoder reqDecoder;

    public ServerMonitorImpl( HttpClientFactory httpClientFactory, RequestDecoder reqDecoder ) {
        this.serverInfoTable = new ConcurrentHashMap<>();
        this.clientFactory = httpClientFactory;
    }

    @Override
    public void pingCacheServers( long currentTime ){
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
        List<Integer> ports = new ArrayList<>(this.serverInfoTable.keySet());

        for (Map.Entry<Integer, ServerInfo> entry : this.serverInfoTable.entrySet()) {
            int port = entry.getKey();
            ServerInfo info = entry.getValue();
            HttpGet req = new HttpGet("127.0.0.1:" + port + "/capacity-factor");

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
        this.serverInfoTable.put(id, new ServerInfo(id, port));
    }
}