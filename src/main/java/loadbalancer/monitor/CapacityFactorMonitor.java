package loadbalancer.monitor;

import org.apache.http.client.HttpClient;

public interface CapacityFactorMonitor {
    void pingServers();
}
