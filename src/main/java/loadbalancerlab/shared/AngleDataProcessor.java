package loadbalancerlab.shared;

import loadbalancerlab.loadbalancer.HashRing;
import loadbalancerlab.loadbalancer.HashRingAngle;

import java.util.*;

public class AngleDataProcessor {
    public String[][] getNumAnglesByTime(SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory) {
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
