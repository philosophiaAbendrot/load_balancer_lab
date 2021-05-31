package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HashRingImpl implements HashRing {
    static int maxAnglesPerServer;
    static int minAnglesPerServer;
    static int defaultAnglesPerServer;
    static int ringSize;

    List<HashRingAngle> angles;
    ConcurrentMap<Integer, List<HashRingAngle>> anglesByServerId;

    public static void configure( Config config) {
        maxAnglesPerServer = config.getMaxAnglesPerServer();
        minAnglesPerServer = config.getMinAnglesPerServer();
        defaultAnglesPerServer = config.getDefaultAnglesPerServer();
        ringSize = config.getRingSize();
    }

    public HashRingImpl() {
        angles = new ArrayList<>();
        anglesByServerId = new ConcurrentHashMap<>();
    }

    @Override
    public int findServerId( String resourceName ) {
        return 0;
    }

    @Override
    public void addAngle( int serverId, int numAngles ) {

    }

    @Override
    public void removeAngle( int serverId, int numAngles ) {

    }
}