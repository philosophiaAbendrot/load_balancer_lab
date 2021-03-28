package loadbalancer.factory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class ClientFactoryImpl implements ClientFactory {
    public CloseableHttpClient buildApacheClient() {
        return HttpClients.createDefault();
    }
}