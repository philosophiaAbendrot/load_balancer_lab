package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Data storage class which is used to store information about CacheServer instances.
 * Implements Cloneable interface to allow copying of class instances.
 */
public class ServerInfo implements Cloneable {

    /**
     * The id of the associated CacheServer.
     */
    private int serverId;

    /**
     * The port that the associated CacheServer is running on.
     */
    private int port;

    /**
     * A record of the history of the associated CacheServer's capacity factor value.
     * Maps the timestamp (seconds since 1-Jan-1970) to the capacity factor at that time.
     */
    private SortedMap<Integer, Double> capFactorRecord;

    /**
     * Time at which server started, in seconds since 1-Jan-1970.
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
     * Constructor
     * @param serverId      The id of the CacheServer.
     * @param port          The port that the CacheServer is running on.
     * @param startTime:    The time at which the CacheServer started running.
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
     * Getter method for capFactorRecord field.
     * @return: returns a copy of capFactorRecord field.
     */
    public SortedMap<Integer, Double> getCapacityFactorRecord() {
        SortedMap<Integer, Double> copyMap = (SortedMap<Integer, Double>) ((TreeMap<Integer, Double>)capFactorRecord).clone();
        return copyMap;
    }

    /**
     * Method from Cloneable interface.
     * @return      Returns cloned copy of self.
     * @throws CloneNotSupportedException       Never thrown.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        ServerInfo copy = (ServerInfo)super.clone();

        /* Make 'capFactorRecord' field hold a copy of the original copy's 'capFactorRecord' field. */
        copy.capFactorRecord = new TreeMap<>(capFactorRecord);

        return copy;
    }

    /**
     * Adds the current snapshot of capacity factor to the capFactorRecord field
     * @param timestamp     Current timestamp in seconds since 1-Jan-1970.
     * @param cf            The current capacity factor (active time / total time).
     */
    public void updateCapacityFactor( int timestamp, double cf ) {
        if (capFactorRecord.containsKey(timestamp)) {

            /* Do nothing if an entry already exists for this timestamp */
            return;
        }

        /* Add new entry */
        capFactorRecord.put(timestamp, cf);
    }

    /**
     * Returns the up-to-date capacity factor of the associated CacheServer object.
     * @return      The capacity factor of the associated CacheServer object.
     */
    public double getCurrentCapacityFactor() {

        /* If there are no records for the capacity factor yet, return 0.0 */
        if (capFactorRecord.isEmpty()) {
            return 0.0;
        }

        int lastTimestamp = capFactorRecord.lastKey();
        return capFactorRecord.get(lastTimestamp);
    }

    /**
     * Getter and setter methods
     */

    /**
     * @return      The start time of the associated CacheServer object.
     */
    public int getStartTime() {
        return startTime;
    }

    /**
     * @return      The deactivation time of the associated CacheServer object.
     */
    public int getDeactivationTime() {
        return deactivationTime;
    }

    /**
     * Sets the deactivation time of the associated CacheServer object.
     * @param deactivationTime      The deactivation time (Seconds since 1-Jan-1970).
     */
    public void setDeactivationTime(int deactivationTime) {
        this.deactivationTime = deactivationTime;
    }

    /**
     * @return      The id of the associated CacheServer.
     */
    public int getServerId() {
        return serverId;
    }

    /**
     * @return      The active status of the associated CacheServer.
     */
    public boolean getActive() { return active; }

    /**
     * Sets the active status of the associated CacheServer.
     * @param active    The active status to be set.
     */
    public void setActive(boolean active) { this.active = active; }

    /**
     * @return      The port which the associated CacheServer object is running on.
     */
    public int getPort() {
        return port;
    }
}