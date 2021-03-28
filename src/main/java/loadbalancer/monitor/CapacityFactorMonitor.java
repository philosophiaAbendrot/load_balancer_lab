package loadbalancer.monitor;
import java.io.IOException;

public interface CapacityFactorMonitor {
    void pingServers() throws IOException;

    int selectPort(int resourceId);

    int startUpBackEnd(int hashRingIndex);
}
