package loadbalancerlab.cacheservermanager;

public interface ServerMonitor {
    void pingCacheServers(long currentTime);

    void addServer(int id, int port);
}
