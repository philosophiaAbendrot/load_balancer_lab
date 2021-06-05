package loadbalancerlab.loadbalancer;

public class ServerInfoImpl {
    int serverId;
    int port;
    double cf;

    public ServerInfoImpl(int _serverId, int _port, double _cf) {
        serverId = _serverId;
        port = _port;
        cf = _cf;
    }

    public int getServerId() {
        return serverId;
    }

    public int getPort() {
        return port;
    }

    public double getCapacityFactor() {
        return cf;
    }
    
    public void setCapacityFactor( double _cf ) {
        cf = _cf;
    }
}