package loadbalancerlab.shared;

import loadbalancerlab.loadbalancer.HashRingAngle;

import java.util.*;

/**
 * Data processing class for processing snapshots of HashRingAngle instances over time into csv-printable information
 */
public class AngleDataProcessor {
    SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory;
    int hashRingSize;
    Integer[] serverIds;
    Integer[] timestamps;

    /**
     * Constructor method
     * @param angleHistory: a table mapping time (seconds since 1-Jan-1970) to a map which holds a snapshot of
     *                      server ids mapping to the HashRingAngle instances belonging to that server at that moment in time
     */
    public AngleDataProcessor(SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory, int hashRingSize) {
        this.angleHistory = angleHistory;
        this.hashRingSize = hashRingSize;

        /* traverse through angle history and compile the ids of every server that was active at one point */
        Set<Integer> serverIdSet = new HashSet<>();

        for (Map<Integer, List<HashRingAngle>> snapshot : angleHistory.values()) {
            for (Integer serverId : snapshot.keySet()) {
                serverIdSet.add(serverId);
            }
        }

        /* convert the set of server ids into an array and sort it in ascending order */
        serverIds = serverIdSet.toArray(new Integer[serverIdSet.size()]);
        Arrays.sort(serverIds);

        /* compile an array of timestamps for serverIds in ascending order */
        timestamps = new Integer[angleHistory.size()];

        int i = 0;
        for (int timestamp : angleHistory.keySet()) {
            timestamps[i++] = timestamp;
        }
    }

    /**
     * Processes angleHistory field into a field counting the number of angles active by server by timestamp
     * @return: Returns a 2d string array formatted for printing to csv holding information about the number of angles
     * for each CacheServer at a number of timestamps
     */
    public String[][] getNumAnglesByTime() {

        /* Create a mapping of index values to server ids */
        Map<Integer, Integer> serverIdTable = new HashMap<>();

        for (int i = 0; i < serverIds.length; i++)
            serverIdTable.put(i, serverIds[i]);

        /* Initialize output graph */
        String[][] outputGraph = new String[angleHistory.size() + 1][serverIds.length + 1];

        /* Fill in header row */
        outputGraph[0][0] = "";

        for (int col = 0; col < serverIds.length; col++)
            outputGraph[0][col + 1] = String.valueOf(serverIds[col]);

        Integer[] timestamps = angleHistory.keySet().toArray(new Integer[angleHistory.size()]);

        /* Fill in rest of graph */
        for (int row = 1; row < outputGraph.length; row++) {

            /* Fill in timestamp in leftmost column */
            int timestamp = timestamps[row - 1];
            outputGraph[row][0] = String.valueOf(timestamp);
            Map<Integer, List<HashRingAngle>> snapShot = angleHistory.get(timestamp);

            /* Fill in other columns */
            for (int col = 1; col < outputGraph[0].length; col++) {
                int serverId = serverIdTable.get(col - 1);

                if (snapShot.containsKey(serverId)) {
                    outputGraph[row][col] = String.valueOf(snapShot.get(serverId).size());
                } else {
                    outputGraph[row][col] = "";
                }
            }
        }

        return outputGraph;
    }

    /**
     * Processes angleHistory field into a field counting the total sweep angle of the HashRingAngle instances associated
     * with each server as a function of time
     * @return: Returns a 2d String array formatted for printing to csv holding information about the total sweep angle for each
     * cache server as a function of time
     */
    public String[][] getSweepAngleByTime() {
        /* For each snapshot in HashRingAngle.angleHistory */
        SortedMap<Integer, Map<Integer, Integer>> sweepAngleHistory = new TreeMap<>();


        for (Map.Entry<Integer, Map<Integer, List<HashRingAngle>>> entry : angleHistory.entrySet()) {
            Integer timestamp = entry.getKey();
            Map<Integer, List<HashRingAngle>> snapshot = entry.getValue();
            /* Find lowest key value of snapshot */
            Set<Integer> anglePositions = snapshot.keySet();

            /* Convert snapshot into a sorted map 'hashRingAngleTable' which maps HashRingAngle position to the HashRingAngle
               instance. */
            SortedMap<Integer, HashRingAngle> hashRingAngleTable = new TreeMap<>();

            for (List<HashRingAngle> angles : snapshot.values()) {
                for (HashRingAngle angle : angles) {
                    hashRingAngleTable.put(angle.getAngle(), angle);
                }
            }

            /* Traverse through each entry in 'hashRingAngleTable' and record how much angle is allocated to each server */
            int prevPos = 0;
            int currentPos;
            int firstPosition = hashRingAngleTable.firstKey();
            int lastPosition = hashRingAngleTable.lastKey();

            Map<Integer, Integer> totalSweepAngleTalliesForSnapshot = new HashMap<>();

            /* Initialize entry for all server id */
            for (Integer serverId : snapshot.keySet()) {
                totalSweepAngleTalliesForSnapshot.put(serverId, 0);
            }

            for (Map.Entry<Integer, HashRingAngle> angleEntry : hashRingAngleTable.entrySet()) {
                HashRingAngle angle = angleEntry.getValue();
                int serverId = angle.getServerId();
                int currentSweepAngleForServer = totalSweepAngleTalliesForSnapshot.get(serverId);
                currentPos = angleEntry.getKey();

                if (angleEntry.getKey() == lastPosition) {

                    /* If the angle is the last angle in the hash ring */
                    totalSweepAngleTalliesForSnapshot.put(serverId, currentSweepAngleForServer + (currentPos - prevPos));
                    HashRingAngle firstAngle = hashRingAngleTable.get(firstPosition);
                    int firstAngleServerId = firstAngle.getServerId();
                    int firstAngleServerCurrentSweepAngle = totalSweepAngleTalliesForSnapshot.get(firstAngleServerId);
                    totalSweepAngleTalliesForSnapshot.put(firstAngleServerId, firstAngleServerCurrentSweepAngle + (hashRingSize - currentPos));
                } else {

                    /* Add the sweep angle from the previous position to the current position */
                    totalSweepAngleTalliesForSnapshot.put(serverId, currentSweepAngleForServer + (currentPos - (prevPos + 1) + 1));
                    int after = totalSweepAngleTalliesForSnapshot.get(serverId);
                    prevPos = currentPos;
                }
            }

            sweepAngleHistory.put(timestamp, totalSweepAngleTalliesForSnapshot);
        }

        /* Convert 'sweepAngleHistory' data into a 2d String for csv-output */
        String[][] outputString = new String[sweepAngleHistory.size() + 1][serverIds.length + 1];

        /* Fill out header row */
        outputString[0][0] = "";
        for (int col = 1; col < outputString[0].length; col++)
            outputString[0][col] = String.valueOf(serverIds[col - 1]);

        /* Fill out other rows */
        for (int row = 1; row < outputString.length; row++) {
            int timestamp = timestamps[row - 1];

            /* Fill out timestamp column */
            outputString[row][0] = String.valueOf(timestamp);

            Map<Integer, Integer> snapshot = sweepAngleHistory.get(timestamp);

            /* Fill out other columns */
            for (int col = 1; col < outputString[0].length; col++) {
                int serverId = serverIds[col - 1];

                if (snapshot.containsKey(serverId)) {
                    int sweepAngle = snapshot.get(serverId);
                    outputString[row][col] = String.valueOf(sweepAngle);
                } else {
                    outputString[row][col] = "";
                }
            }
        }

        return outputString;
    }
}
