package loadbalancer.factory;

import org.apache.http.impl.client.CloseableHttpClient;

public interface ClientFactory {
    CloseableHttpClient buildApacheClient();
}
