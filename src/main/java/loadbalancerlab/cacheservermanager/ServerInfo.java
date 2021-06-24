package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;
import java.util.TreeMap;

public class ServerInfo {
    private int serverId;
    private int port;
    private SortedMap<Integer, Double> capFactorRecord;
    /**
     * Time at which server started, in seconds since 1-Jan-1970
     */
    private int startTime;
    /**
     * Time at which server deactivated, in seconds since 1-Jan-1970
     */
    private int deactivationTime;
    private boolean active;

    public ServerInfo( int serverId, int port, int currentTime ) {
        startTime = currentTime;
        this.serverId = serverId;
        this.port = port;
        capFactorRecord = new TreeMap<>();
        active = true;
        deactivationTime = -1;
    }

    public SortedMap<Integer, Double> getCapacityFactorRecord() {
        SortedMap<Integer, Double> copyMap = (SortedMap<Integer, Double>) ((TreeMap<Integer, Double>)capFactorRecord).clone();
        return copyMap;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getDeactivationTime() {
        return deactivationTime;
    }

    public void setDeactivationTime(int deactivationTime) {
        this.deactivationTime = deactivationTime;
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