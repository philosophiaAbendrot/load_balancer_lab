package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HashRing {
    static int maxAnglesPerServer;
    static int minAnglesPerServer;
    static int defaultAnglesPerServer;
    static int ringSize;
    static HashFunction hashFunction;

    ConcurrentMap<Integer, HashRingAngle> angles;
    ConcurrentMap<Integer, List<HashRingAngle>> anglesByServerId;
    SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory;

    public static void configure( Config config ) {
        maxAnglesPerServer = config.getMaxAnglesPerServer();
        minAnglesPerServer = config.getMinAnglesPerServer();
        defaultAnglesPerServer = config.getDefaultAnglesPerServer();
        ringSize = config.getRingSize();
        hashFunction = config.getHashFunction();
    }

    public HashRing() {
        angles = new ConcurrentHashMap<>();
        anglesByServerId = new ConcurrentHashMap<>();
        angleHistory = new TreeMap<>();
    }

    public SortedMap<Integer, Map<Integer, List<HashRingAngle>>> getHashRingAngleHistory() {
        return angleHistory;
    }

    public int findServerId( String resourceName ) {
        int resourcePosition = hashFunction.hash(resourceName) % ringSize;

        int lowestAngle = Integer.MAX_VALUE;
        int idLowestAngle = -1;

        int lowestAngleHigherThanPos = Integer.MAX_VALUE;
        int idLowestAngleHigherThanPos = -1;

        for (Map.Entry<Integer, HashRingAngle> entry : angles.entrySet()) {
            int angle = entry.getKey();
            int serverId = entry.getValue().getServerId();

            if (angle < lowestAngle) {
                lowestAngle = angle;
                idLowestAngle = serverId;
            }

            if (angle > resourcePosition && angle < lowestAngleHigherThanPos) {
                lowestAngleHigherThanPos = angle;
                idLowestAngleHigherThanPos = serverId;
            }
        }

        if (idLowestAngleHigherThanPos == -1) {
            return idLowestAngle;
        } else {
            return idLowestAngleHigherThanPos;
        }
    }

    public void addAngle( int serverId, int numAngles ) {
        Random rand = new Random();

        if (!anglesByServerId.containsKey(serverId))
            throw new IllegalArgumentException("entry for server id " + serverId + " does not exist");

        for (int i = 0; i < numAngles; i++) {
            if (anglesByServerId.get(serverId).size() >= maxAnglesPerServer)
                break;

            int angle;

            while (true) {
                angle = rand.nextInt(ringSize);

                if (!angles.containsKey(angle)) {
                    break;
                }
            }

            HashRingAngle newAngle = new HashRingAngle(serverId, angle);
            anglesByServerId.get(serverId).add(newAngle);
            angles.put(angle, newAngle);
        }
    }

    public void removeAngle( int serverId, int numAngles ) {
        Random rand = new Random();

        if (!anglesByServerId.containsKey(serverId))
            throw new IllegalArgumentException("entry for server id " + serverId + " does not exist");

        for (int i = 0; i < numAngles; i++) {
            if (anglesByServerId.get(serverId).size() <= minAnglesPerServer)
                break;

            List<HashRingAngle> angleList = anglesByServerId.get(serverId);

            int randIdx = rand.nextInt(angleList.size());
            int selectedAngle = angleList.get(randIdx).getAngle();

            angleList.remove(randIdx);
            angles.remove(selectedAngle);
        }
    }

    public void addServer( int serverId ) {
        if (anglesByServerId.containsKey(serverId))
            throw new IllegalArgumentException("Server with id = " + serverId + " is already recorded in HashRingImpl");

        anglesByServerId.put(serverId, new ArrayList<>());
        addAngle(serverId, defaultAnglesPerServer);
    }

    /**
     * Records a copy of anglesByServerId for a particular moment in time into 'angleHistory' field
     * @param currentTime: the current time, in seconds since 1-Jan-1970
     */
    public void recordServerAngles(int currentTime) {
        // do not record if there is already an entry for the given timestamp
        if (angleHistory.containsKey(currentTime))
            return;

        Map<Integer, List<HashRingAngle>> copyTable = new HashMap<>();

        anglesByServerId.forEach((k, v) -> {
            List<HashRingAngle> newList = new ArrayList<>(v);
            copyTable.put(k, newList);
        });

        angleHistory.put(currentTime, copyTable);
    }

    public void removeServer( int serverId ) {
        if (!anglesByServerId.containsKey(serverId))
            throw new IllegalArgumentException("Server with id = " + serverId + " is not recorded in HashRingImpl");

        List<HashRingAngle> angleList = anglesByServerId.get(serverId);

        for (HashRingAngle angle : angleList)
            angles.remove(angle.getAngle());

        anglesByServerId.remove(serverId);
    }
}