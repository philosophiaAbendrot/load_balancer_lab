package loadbalancerlab.loadbalancer;

public interface CacheRedistributor {
    void requestServerInfo();

    int selectPort(String resourceName);

    void remapCacheKeys();
}
