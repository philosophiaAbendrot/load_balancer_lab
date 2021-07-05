package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Data storage class which is used to store information about CacheServer instances.
 * Implements Cloneable interface to allow cloning of class instances.
 */
public class ServerInfo implements Cloneable {
    /**
     * The id of the associated CacheServer
     */
    private int serverId;
    /**
     * The port that the associated CacheServer is running on
     */
    private int port;
    /**
     * A record of the history of the associated CacheServer's capacity factor value
     * Maps the timestamp (seconds since 1-Jan-1970) to the capacity factor at that time
     */
    private SortedMap<Integer, Double> capFactorRecord;
    /**
     * Time at which server started, in seconds since 1-Jan-1970
     */
    private int startTime;
    /**
     * Time at which CacheServer deactivated, in seconds since 1-Jan-1970
     */
    private int deactivationTime;
    /**
     * CacheServer's current active status
     */
    private boolean active;

    /**
     * @param serverId: the id of the CacheServer
     * @param port: the port that the CacheServer is running on
     * @param startTime: the time at which the CacheServer started running
     */
    public ServerInfo( int serverId, int port, int startTime ) {
        this.startTime = startTime;
        this.serverId = serverId;
        this.port = port;
        capFactorRecord = new TreeMap<>();
        active = true;
        deactivationTime = -1;
    }

    /**
     * Getter method for capFactorRecord field
     * @return: returns a copy of capFactorRecord field
     */
    public SortedMap<Integer, Double> getCapacityFactorRecord() {
        SortedMap<Integer, Double> copyMap = (SortedMap<Integer, Double>) ((TreeMap<Integer, Double>)capFactorRecord).clone();
        return copyMap;
    }

    /**
     * Method from Cloneable interface
     * @return: Returns cloned copy of self
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        ServerInfo copy = (ServerInfo)super.clone();
        copy.capFactorRecord = new TreeMap<>(capFactorRecord);

        return copy;
    }

    /**
     * Adds the current snapshot of capacity factor to the capFactorRecord field
     * @param timestamp: current timestamp in seconds since 1-Jan-1970
     * @param cf: the current capacity factor (active time / total time)
     */
    public void updateCapacityFactor( int timestamp, double cf ) {
        if (capFactorRecord.containsKey(timestamp)) {
            // do nothing if an entry already exists for this timestamp
            return;
        }

        // add new entry
        capFactorRecord.put(timestamp, cf);
    }

    /**
     * @return: the most up to date capacity factor value
     */
    public double getCurrentCapacityFactor() {
        if (capFactorRecord.isEmpty()) {
            return 0.0;
        }

        int lastTimestamp = capFactorRecord.lastKey();
        return capFactorRecord.get(lastTimestamp);
    }

    /**
     * Getter and setter methods
     */
    public int getStartTime() {
        return startTime;
    }

    public int getDeactivationTime() {
        return deactivationTime;
    }

    public void setDeactivationTime(int deactivationTime) {
        this.deactivationTime = deactivationTime;
    }

    public int getServerId() {
        return serverId;
    }

    public boolean getActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public int getPort() {
        return port;
    }
}