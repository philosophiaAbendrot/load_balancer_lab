package loadbalancerlab.loadbalancer;

public class HashRingAngleImpl implements HashRingAngle {
    private int serverId;
    private int angle;
    private HashFunction hashFunction;

    public HashRingAngleImpl(int _serverId, int _angle, HashFunction _hashFunction) {
        angle = _angle;
        serverId = _serverId;
        hashFunction = _hashFunction;
    }

    @Override
    public int getServerId() {
        return serverId;
    }

    @Override
    public int getAngle() {
        return angle;
    }
}