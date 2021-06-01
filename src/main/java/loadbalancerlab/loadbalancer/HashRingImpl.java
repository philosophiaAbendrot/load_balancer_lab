package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HashRingImpl implements HashRing {
    static int maxAnglesPerServer;
    static int minAnglesPerServer;
    static int defaultAnglesPerServer;
    static int ringSize;

    ConcurrentMap<Integer, HashRingAngle> angles;
    ConcurrentMap<Integer, List<HashRingAngle>> anglesByServerId;

    public static void configure( Config config) {
        maxAnglesPerServer = config.getMaxAnglesPerServer();
        minAnglesPerServer = config.getMinAnglesPerServer();
        defaultAnglesPerServer = config.getDefaultAnglesPerServer();
        ringSize = config.getRingSize();
    }

    public HashRingImpl() {
        angles = new ConcurrentHashMap<>();
        anglesByServerId = new ConcurrentHashMap<>();
    }

    @Override
    public int findServerId( String resourceName ) {
        return 0;
    }

    @Override
    public void addAngle( int serverId, int numAngles ) {
        Random rand = new Random();

        if (!anglesByServerId.containsKey(serverId))
            throw new IllegalArgumentException("entry for server id " + serverId + " does not exist");

        for (int i = 0; i < numAngles; i++) {
            if (anglesByServerId.get(serverId).size() >= maxAnglesPerServer)
                break;

            int angle;

            Comparator<HashRingAngle> comparator = new Comparator<HashRingAngle>() {
                @Override
                public int compare( HashRingAngle a1, HashRingAngle a2 ) {
                    return a1.getAngle() - a2.getAngle();
                }
            };

            while (true) {
                angle = rand.nextInt(ringSize);

                if (!angles.containsKey(angle)) {
                    break;
                }
            }

            HashRingAngle newAngle = new HashRingAngleImpl(serverId, angle);
            anglesByServerId.get(serverId).add(newAngle);
            angles.put(angle, newAngle);
        }
    }

    @Override
    public void removeAngle( int serverId, int numAngles ) {

    }

    @Override
    public void addServer( int serverId ) {
        if (anglesByServerId.containsKey(serverId))
            throw new IllegalArgumentException("Server with id = " + serverId + " is already recorded in HashRingImpl");

        anglesByServerId.put(serverId, new ArrayList<>());
        addAngle(serverId, defaultAnglesPerServer);
    }

    @Override
    public void removeServer( int serverId ) {

    }
}