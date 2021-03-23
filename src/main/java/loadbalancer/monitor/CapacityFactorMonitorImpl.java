package loadbalancer.monitor;

import org.apache.http.client.HttpClient;

public class CapacityFactorMonitorImpl {
    HttpClient client;

    public CapacityFactorMonitorImpl(HttpClient client) {
        this.client = client;

    }
}