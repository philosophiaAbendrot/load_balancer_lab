package loadbalancerlab.loadbalancer;

public interface CacheRedistributor {
    void requestServerInfo();

    int selectPort(String resourceName) throws IllegalStateException;

    void remapCacheKeys();
}
