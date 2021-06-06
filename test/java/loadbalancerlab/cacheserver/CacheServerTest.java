package loadbalancerlab.cacheserver;

import loadbalancerlab.shared.Logger;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CacheServerTest {
    private CacheServer cacheServer;
    private Thread cacheServerThread;
    int cacheServerPort;

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.THREAD_MANAGEMENT });
    }

    @BeforeEach
    public void setup() {
        cacheServer = new CacheServer(new RequestMonitor("CacheServer"));
        cacheServerThread = new Thread(cacheServer);
        cacheServerThread.start();

        // wait for cacheServerThread to startup and select a port
        while (true) {
            try {
                Thread.sleep(100);
                cacheServerPort = cacheServer.getPort();

                if ((cacheServerPort = cacheServer.getPort()) != 0) {
                    break;
                }
            } catch (InterruptedException e) { }
        }
    }

    @Test
    @DisplayName("Test that cache server is running and accepting requests")
    public void checkCacheServerRunning() {
        int status = -1;

        try {
            // send a request to the cache server port
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet getRequest = new HttpGet("http://127.0.0.1:" + cacheServerPort);
            CloseableHttpResponse response = httpClient.execute(getRequest);
            status = response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cacheServerThread.interrupt();
        }

        assertEquals(200, status);
    }

    @Nested
    @DisplayName("When cache server is interrupted")
    class WhenCacheServerIsInterrupted {
        Thread mockReqMonitorThread;

        @BeforeEach
        public void setup() {
            mockReqMonitorThread = Mockito.mock(Thread.class);
            cacheServer.reqMonitorThread.interrupt();
            cacheServer.reqMonitorThread = mockReqMonitorThread;
            cacheServerThread.interrupt();
        }

        @Test
        @DisplayName("request monitor thread should be interrupted")
        public void reqMonitorThreadShouldBeInterrupted() {
            verify(mockReqMonitorThread, times(1)).interrupt();
        }
    }
}