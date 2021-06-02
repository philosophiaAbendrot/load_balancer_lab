package loadbalancerlab.loadbalancer;

public interface ServerInfo {
    int getServerId();
    int getPort();
    double getCapacityFactor();
    void setCapacityFactor(double cf);
}
