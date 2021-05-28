package loadbalancer.services.monitor;
import java.io.IOException;

public interface CapacityFactorMonitor {
    void pingServers(long currentTime) throws IOException;

    int selectPort(int resourceId);

    int startupCacheServer(int hashRingIndex);

    void shutdownCacheServer(int cacheServerPort);
}
