package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;
import java.util.TreeMap;

public class ServerInfo {
    private int serverId;
    private int port;
    private SortedMap<Integer, Double> capFactorRecord;
    private boolean active;

    public ServerInfo( int serverId, int port ) {
        this.serverId = serverId;
        this.port = port;
        capFactorRecord = new TreeMap<>();
        active = true;
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

    public boolean getActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

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