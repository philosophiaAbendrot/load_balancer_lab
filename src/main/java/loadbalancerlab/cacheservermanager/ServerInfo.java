package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;
import java.util.TreeMap;

public class ServerInfo {
    private int serverId;
    private int port;
    private SortedMap<Integer, Double> capFactorRecord;

    public ServerInfo( int _serverId, int _port ) {
        this.serverId = _serverId;
        this.port = _port;
        capFactorRecord = new TreeMap<>();
    }

    public SortedMap<Integer, Double> getCapacityFactorRecord() {
        SortedMap<Integer, Double> copyMap = (SortedMap<Integer, Double>) ((TreeMap<Integer, Double>)capFactorRecord).clone();
        return copyMap;
    }

    public double getCurrentCapacityFactor() {
        if (capFactorRecord.isEmpty()) {
            return 0.0;
        }

        int lastTimestamp = capFactorRecord.lastKey();
        return capFactorRecord.get(lastTimestamp);
    }

    public int getServerId() {
        return serverId;
    }

    public int getPort() {
        return port;
    }

    public void updateCapacityFactor( int timestamp, double cf ) {
        if (capFactorRecord.containsKey(timestamp)) {
            // do nothing if an entry already exists for this timestamp
            return;
        }

        // add new entry
        capFactorRecord.put(timestamp, cf);
    }
}