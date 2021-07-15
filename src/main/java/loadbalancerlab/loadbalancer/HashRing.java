package loadbalancerlab.loadbalancer;

import loadbalancerlab.shared.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A class which implements a consistent hashing mechanism to map resource names to CacheServer objects.
 */
public class HashRing {

    /**
     * Controls the maximum number of HashRingAngle objects per CacheServer object.
     */
    static int maxAnglesPerServer;

    /**
     * Controls the minimum number of HashRingAngle objects per CacheServer instance.
     */
    static int minAnglesPerServer;

    /**
     * Controls the default number of HashRingAngle objects per CacheServer object.
     */
    static int defaultAnglesPerServer;

    /**
     * Controls the number of positions on HashRing objects.
     */
    static int ringSize;

    /**
     * The HashFunction object which is used to hash resource names into positions.
     */
    static HashFunction hashFunction;

    /**
     * A hash table which maps integer position values to all the HashRingAngle objects which the positions contain.
     */
    ConcurrentMap<Integer, HashRingAngle> angles;

    /**
     * A hash table which maps CacheServer ids to a list of HashRingAngle instances that belong to that server.
     */
    ConcurrentMap<Integer, List<HashRingAngle>> anglesByServerId;

    /**
     * Contains snapshots of anglesByServerId at specific timestamps.
     * Maps the timestamps to the snapshots.
     */
    SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory;

    /**
     * Method used to configure static variables.
     * @param config    a Config object used to configure various classes.
     */
    public static void configure( Config config ) {
        maxAnglesPerServer = config.getMaxAnglesPerServer();
        minAnglesPerServer = config.getMinAnglesPerServer();
        defaultAnglesPerServer = config.getDefaultAnglesPerServer();
        ringSize = config.getRingSize();
        hashFunction = config.getHashFunction();
    }

    /**
     * Constructor
     */
    public HashRing() {
        angles = new ConcurrentHashMap<>();
        anglesByServerId = new ConcurrentHashMap<>();
        angleHistory = new TreeMap<>();
    }

    /**
     * Getter method for angleHistory field
     * @return      Returns angleHistory field which maps timestamps to snapshots of the 'anglesByServerId' field at
     *              those times.
     */
    public SortedMap<Integer, Map<Integer, List<HashRingAngle>>> getHashRingAngleHistory() {
        return angleHistory;
    }

    /**
     * @param resourceName      The name of the resource being accessed.
     * @return                  The id of the CacheServer which is responsible for that resource.
     */
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

    /**
     * Generates 'numAngles' HashRingAngle instances under CacheServer with id 'serverId'.
     * @param serverId      The id of the CacheServer object to which HashRingAngle objects are being added.
     * @param numAngles     The number of HashRingAngle objects which are being added.
     * @throws IllegalArgumentException     Thrown if there is no entry in 'anglesByServerId' field for the serverId.
     */
    public void addAngle( int serverId, int numAngles ) throws IllegalArgumentException {
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

            /*  Update anglesByServerId field with new HashRingAngle object */
            anglesByServerId.get(serverId).add(newAngle);

            /*  Update angles field with new HashRingAngle object */
            angles.put(angle, newAngle);
        }
    }

    /**
     * Removes 'numAngles' HashRingAngle instances from CacheServer object with id 'serverId'.
     * @param serverId      The id of the CacheServer instance.
     * @param numAngles     The number of HashRingAngle objects being removed.
     * @throws IllegalArgumentException         Thrown when there is no entry for CacheServer with id 'serverId' in
     *                                          'anglesByServerId' field.
     */
    public void removeAngle( int serverId, int numAngles ) throws IllegalArgumentException {
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

    /**
     * Adds an entry for CacheServer with id 'serverId' in 'anglesByServerId' field.
     * @param serverId      The id of the CacheServer instance.
     */
    public void addServer( int serverId ) {
        if (anglesByServerId.containsKey(serverId))
            throw new IllegalArgumentException("Server with id = " + serverId + " is already recorded in HashRingImpl");

        anglesByServerId.put(serverId, new ArrayList<>());
        addAngle(serverId, defaultAnglesPerServer);
    }

    /**
     * Records a copy of anglesByServerId for a particular moment in time into 'anglesByServerId' field.
     * @param currentTime   The current time, in seconds since 1-Jan-1970.
     */
    public void recordServerAngles(int currentTime) {

        /* Do not record if there is already an entry for the given timestamp */
        if (angleHistory.containsKey(currentTime))
            return;

        Map<Integer, List<HashRingAngle>> copyTable = new HashMap<>();

        anglesByServerId.forEach((k, v) -> {
            List<HashRingAngle> newList = new ArrayList<>(v);
            copyTable.put(k, newList);
        });

        angleHistory.put(currentTime, copyTable);
    }

    /**
     * Removes entry of CacheServer object with id 'serverId' from 'anglesByServerId' field.
     * @param serverId      The id of the CacheServer object being removed.
     */
    public void removeServer( int serverId ) {
        if (!anglesByServerId.containsKey(serverId))
            throw new IllegalArgumentException("Server with id = " + serverId + " is not recorded in HashRingImpl");

        List<HashRingAngle> angleList = anglesByServerId.get(serverId);

        for (HashRingAngle angle : angleList)
            angles.remove(angle.getAngle());

        anglesByServerId.remove(serverId);
    }
}