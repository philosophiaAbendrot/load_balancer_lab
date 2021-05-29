package loadbalancerlab.cacheservermanager;

public class ServerInfo {
    public int id;
    public int port;
    public double capacityFactor;

    public ServerInfo( int id, int port ) {
        this.id = id;
        this.port = port;
    }
}