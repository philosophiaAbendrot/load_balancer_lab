package loadbalancerlab.loadbalancer;

/**
 * A class which is used by objects of the HashRing class for its consistent hashing logic
 * HashRingAngle objects are placed in 'angles' on the HashRing object and have an associated CacheServer object
 */
public class HashRingAngle {
    /**
     * The id of the CacheServer object that the HashRingAngle object belongs to
     */
    private int serverId;
    /**
     * The position that the HashRingAngle object is located on the HashRing
     */
    private int angle;

    /**
     * @param serverId      The id of the CacheServer object that the HashRingAngle object belongs to
     * @param angle         The angle position that the HashRingAngle object is placed on the HashRing
     */
    public HashRingAngle( int serverId, int angle) {
        this.angle = angle;
        this.serverId = serverId;
    }

    /**
     * Getter method for serverId
     * @return      Returns the id of the CacheServer object that the HashRingAngle object belogns to
     */
    public int getServerId() {
        return serverId;
    }

    /**
     * Getter method for angle
     * @return      Returns the angle that the HashRingAngle object is placed in on the HashRing
     */
    public int getAngle() {
        return angle;
    }

    /**
     * Setter method for serverId
     * @param serverId      The id of the CacheServer object this HashRingAngle object belongs to
     */
    public void setServerId(int serverId) { this.serverId = serverId; }
}