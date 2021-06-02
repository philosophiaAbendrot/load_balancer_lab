package loadbalancerlab.loadbalancer;

public class ServerInfoImpl implements ServerInfo {
    int serverId;
    int port;
    double cf;

    public ServerInfoImpl(int _serverId, int _port, double _cf) {
        serverId = _serverId;
        port = _port;
        cf = _cf;
    }

    @Override
    public int getServerId() {
        return serverId;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public double getCapacityFactor() {
        return cf;
    }

    @Override
    public void setCapacityFactor( double _cf ) {
        cf = _cf;
    }
}