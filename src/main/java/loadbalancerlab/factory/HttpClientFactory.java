package loadbalancerlab.factory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Factory class used for generating CloseableHttpClient instances.
 */
public class HttpClientFactory {

    /**
     * @return A CloseableHttpClient instance used to send http requests.
     */
    public CloseableHttpClient buildApacheClient() {
        return HttpClients.createDefault();
    }
}