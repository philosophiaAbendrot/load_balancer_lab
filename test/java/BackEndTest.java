import loadbalancer.monitor.RequestMonitor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.IOException;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class BackEndTest {
    private BackEnd backend;
    private Thread backendThread;

    @Test
    @DisplayName("Test that backend server is running and accepting requests")
    public void checkBackendServerRunning() {
        backend = new BackEnd(new RequestMonitor("BackEnd"));
        backendThread = new Thread(backend);
        int status = -1;

        try {
            backendThread.start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {  }

            int port = backend.port;

            // send a request to the backend port
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet getRequest = new HttpGet("http://127.0.0.1:" + port);
            CloseableHttpResponse response = httpClient.execute(getRequest);
            status = response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            backendThread.interrupt();
        }

        assertEquals(200, status, "Backend server is not running and accepting requests");
    }
}