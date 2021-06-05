package loadbalancerlab.loadbalancer;

public class HashRingAngle {
    private int serverId;
    private int angle;
    private HashFunction hashFunction;

    public HashRingAngle( int _serverId, int _angle, HashFunction _hashFunction) {
        angle = _angle;
        serverId = _serverId;
        hashFunction = _hashFunction;
    }

    public int getServerId() {
        return serverId;
    }

    public int getAngle() {
        return angle;
    }
}