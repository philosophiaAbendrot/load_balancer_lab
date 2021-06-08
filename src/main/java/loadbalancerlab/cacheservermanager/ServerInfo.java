package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;
import java.util.TreeMap;

public class ServerInfo {
    private int serverId;
    private int port;
    private SortedMap<Integer, Double> capFactorRecord;
    static int cfRecordSize = 10;

    public ServerInfo( int _serverId, int _port ) {
        this.serverId = _serverId;
        this.port = _port;
        capFactorRecord = new TreeMap<>();
    }

    public SortedMap<Integer, Double> getCapacityFactorRecord() {
        SortedMap<Integer, Double> copyMap = (SortedMap<Integer, Double>) ((TreeMap<Integer, Double>)capFactorRecord).clone();
        return copyMap;
    }

    public double getAverageCapacityFactor() {
        if (capFactorRecord.isEmpty())
            return 0.0;

        double totalCf = 0;

        for (Double cf : capFactorRecord.values()) {
            totalCf += cf;
        }

        return totalCf / capFactorRecord.size();
    }

    public int getServerId() {
        return serverId;
    }

    public int getPort() {
        return port;
    }

    public void updateCapacityFactor( int timestamp, double cf ) {
        System.out.println("ServerInfo | updateCapacityFactor being called with timestamp = " + timestamp + " cf = " + cf);

        if (capFactorRecord.containsKey(timestamp)) {
            // do nothing if an entry already exists for this timestamp
            return;
        }

        // prevent cap factor record size from increasing past cfRecordSize
        if (capFactorRecord.size() >= cfRecordSize) {
            int lowestKey = capFactorRecord.firstKey();
            capFactorRecord.remove(lowestKey);
        }

        // add new entry
        capFactorRecord.put(timestamp, cf);
    }
}