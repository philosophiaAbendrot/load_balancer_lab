package loadbalancerlab.factory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpClientFactory {
    public CloseableHttpClient buildApacheClient() {
        return HttpClients.createDefault();
    }
}