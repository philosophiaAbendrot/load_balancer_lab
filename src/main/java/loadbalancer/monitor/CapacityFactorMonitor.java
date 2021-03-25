package loadbalancer.monitor;

import org.apache.http.client.HttpClient;

import java.io.IOException;

public interface CapacityFactorMonitor {
    void pingServers() throws IOException;
}
