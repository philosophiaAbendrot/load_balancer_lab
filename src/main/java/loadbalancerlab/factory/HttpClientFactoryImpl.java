package loadbalancerlab.factory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpClientFactoryImpl {
    public CloseableHttpClient buildApacheClient() {
        return HttpClients.createDefault();
    }
}