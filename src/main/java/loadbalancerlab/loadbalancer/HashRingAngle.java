package loadbalancerlab.loadbalancer;

public class HashRingAngle {
    private int serverId;
    private int angle;

    public HashRingAngle( int _serverId, int _angle) {
        angle = _angle;
        serverId = _serverId;
    }

    public int getServerId() {
        return serverId;
    }

    public int getAngle() {
        return angle;
    }

    public void setServerId(int serverId) { this.serverId = serverId; }
}