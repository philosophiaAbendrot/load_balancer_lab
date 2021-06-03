package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

public class CacheRedistributorImpl implements CacheRedistributor {
    Map<Integer, ServerInfo> serverInfoTable;
    HashRing hashRing;

    static double targetCapacityFactor;
    private static RequestDecoder reqDecoder;
    private int cacheServerManagerPort;
    private static HttpClientFactory clientFactory;

    public static void configure( Config config ) {
        targetCapacityFactor = config.getTargetCapacityFactor();
        reqDecoder = config.getRequestDecoder();
        clientFactory = config.getClientFactory();
    }

    public CacheRedistributorImpl(int _cacheServerManagerPort, HashRing _hashRing) {
        serverInfoTable = new HashMap<>();
        cacheServerManagerPort = _cacheServerManagerPort;
        hashRing = _hashRing;
    }

    // sends request to cache server manager for an update on cache servers
    // updates the serverInfoTable field using the results
    // params: currentTime - the current time in seconds since Jan 1 1970
    @Override
    public void requestServerInfo( long currentTime ) {

    }

    @Override
    public int selectPort( String resourceName ) {
        return 0;
    }

    @Override
    public void remapCacheKeys() {

    }
}