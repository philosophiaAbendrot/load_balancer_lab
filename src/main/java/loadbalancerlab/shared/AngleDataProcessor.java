package loadbalancerlab.shared;

import loadbalancerlab.loadbalancer.HashRingAngle;

import java.util.*;

/**
 * Data processing class for processing snapshots of HashRingAngle instances over time into csv-printable information
 */
public class AngleDataProcessor {
    SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory;

    /**
     * Constructor method
     * @param angleHistory: a table mapping time (seconds since 1-Jan-1970) to a map which holds a snapshot of
     *                      server ids mapping to the HashRingAngle instances belonging to that server at that moment in time
     */
    public AngleDataProcessor(SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory) {
        this.angleHistory = angleHistory;
    }

    /**
     * Processes angleHistory field into a field counting the number of angles active by server by timestamp
     * @return: Returns a 2d string array formatted for printing to csv holding information about the number of angles
     * for each CacheServer at a number of timestamps
     */
    public String[][] getNumAnglesByTime() {
        int maxServerCount = 0;
        int maxServerCountTimestamp = 0;

        for (Map.Entry<Integer, Map<Integer, List<HashRingAngle>>> entry : angleHistory.entrySet()) {
            int numServers;

            if ((numServers = entry.getValue().size()) > maxServerCount) {
                maxServerCount = numServers;
                maxServerCountTimestamp = entry.getKey();
            }
        }

        // find list of all server ids
        Integer[] serverIds = angleHistory.get(maxServerCountTimestamp).keySet().toArray(new Integer[maxServerCount]);
        Arrays.sort(serverIds);

        // create a mapping of index values to server ids
        Map<Integer, Integer> serverIdTable = new HashMap<>();

        for (int i = 0; i < serverIds.length; i++)
            serverIdTable.put(i, serverIds[i]);

        // initialize output graph
        String[][] outputGraph = new String[angleHistory.size() + 1][maxServerCount + 1];

        // fill in header row
        outputGraph[0][0] = "";

        for (int col = 0; col < serverIds.length; col++)
            outputGraph[0][col + 1] = String.valueOf(serverIds[col]);

        Integer[] timestamps = angleHistory.keySet().toArray(new Integer[angleHistory.size()]);

        // fill in rest of graph
        for (int row = 1; row < outputGraph.length; row++) {
            // fill in timestamp in leftmost column
            int timestamp = timestamps[row - 1];
            outputGraph[row][0] = String.valueOf(timestamp);
            Map<Integer, List<HashRingAngle>> snapShot = angleHistory.get(timestamp);

            // fill in other columns
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
}
