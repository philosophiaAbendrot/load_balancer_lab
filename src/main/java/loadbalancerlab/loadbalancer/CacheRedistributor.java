package loadbalancerlab.loadbalancer;

public interface CacheRedistributor {
    void requestServerInfo(long currentTime);

    int selectPort(String resourceName);

    void remapCacheKeys();
}
