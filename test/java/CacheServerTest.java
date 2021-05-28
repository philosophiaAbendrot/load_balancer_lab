import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.IOException;

import loadbalancerlab.services.monitor.RequestMonitor;
import loadbalancerlab.cacheserver.CacheServer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CacheServerTest {
    private CacheServer cacheServer;
    private Thread cacheServerThread;

    @Test
    @DisplayName("Test that cache server is running and accepting requests")
    public void checkCacheServerRunning() {
        cacheServer = new CacheServer(new RequestMonitor("CacheServer"));
        cacheServerThread = new Thread(cacheServer);
        int status = -1;

        try {
            cacheServerThread.start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {  }

            int port = cacheServer.port;

            // send a request to the cache server port
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet getRequest = new HttpGet("http://127.0.0.1:" + port);
            CloseableHttpResponse response = httpClient.execute(getRequest);
            status = response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cacheServerThread.interrupt();
        }

        assertEquals(200, status, "Cache server is not running and accepting requests");
    }
}