package loadbalancer.factory;

import org.apache.http.impl.client.CloseableHttpClient;

public class ClientFactoryImpl implements ClientFactory {
    public CloseableHttpClient buildApacheClient() {
        return null;
    }
}