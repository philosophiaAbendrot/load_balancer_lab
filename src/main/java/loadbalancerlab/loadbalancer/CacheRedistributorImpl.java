package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;

import java.util.HashMap;
import java.util.Map;

public class CacheRedistributorImpl implements CacheRedistributor {
    Map<Integer, ServerInfo> serverInfoTable;
    HashRing hashRing;
    static double targetCapacityFactor;
    private static RequestDecoder reqDecoder;
    private int cacheServerManagerPort;

    public static void configure( Config config ) {
        targetCapacityFactor = config.getTargetCapacityFactor();
        reqDecoder = config.getRequestDecoder();
    }

    public CacheRedistributorImpl(int _cacheServerManagerPort) {
        serverInfoTable = new HashMap<>();
        cacheServerManagerPort = _cacheServerManagerPort;
    }

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