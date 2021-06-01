package loadbalancerlab.loadbalancer;

public class HashRingAngleImpl implements HashRingAngle, Comparable {
    int serverId;
    int angle;

    public HashRingAngleImpl(int _serverId, int _angle) {
        angle = _angle;
        serverId = _serverId;
    }

    @Override
    public int getServerId() {
        return serverId;
    }

    @Override
    public int getAngle() {
        return angle;
    }

    @Override
    public int compareTo( Object o ) {
        HashRingAngle casted = (HashRingAngle)o;
        return angle - casted.getAngle();
    }
}