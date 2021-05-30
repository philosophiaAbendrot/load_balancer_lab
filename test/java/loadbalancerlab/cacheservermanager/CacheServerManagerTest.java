package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactoryImpl;
import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.services.monitor.RequestMonitor;
import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoder;
import loadbalancerlab.shared.RequestDecoderImpl;

import loadbalancerlab.shared.ServerInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.apache.http.impl.client.HttpClients;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CacheServerManagerTest {
    CacheServerManager cacheServerManager;
    Thread cacheServerMonitorThread;
    ServerMonitor serverMonitor;

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
    }

    @Nested
    @DisplayName("Testing with a mock cache server")
    public class MockCacheServerTests {
        CacheServerFactory mockFactory;
        CacheServer mockCacheServer;
        Thread mockCacheServerThread;
        int cacheServerMonitorPort;

        @BeforeEach
        public void setup() {
            mockFactory = Mockito.mock(CacheServerFactoryImpl.class);
            mockCacheServer = Mockito.mock(CacheServer.class);
            mockCacheServer.port = 37_100;
            mockCacheServerThread = Mockito.mock(Thread.class);

            when(mockFactory.produceCacheServer(any(RequestMonitor.class))).thenReturn(mockCacheServer);
            when(mockFactory.produceCacheServerThread(any(CacheServer.class))).thenReturn(mockCacheServerThread);

            cacheServerManager = new CacheServerManager(mockFactory, new HttpClientFactoryImpl(), new RequestDecoderImpl());
            cacheServerMonitorThread = new Thread(cacheServerManager);
            cacheServerMonitorThread.start();
            cacheServerMonitorPort = CacheServerManagerTest.waitUntilServerReady(cacheServerManager);
        }

        @Nested
        @DisplayName("Testing startupCacheServer()")
        public class TestingStartupCacheServer {
            @Test
            @DisplayName("should startup cache servers")
            public void shouldStartCacheServerInstances() {
                int num = 5;
                cacheServerManager.startupCacheServer(num);
                verify(mockFactory, times(num)).produceCacheServer(any(RequestMonitor.class));
            }

            @Test
            @DisplayName("should startup cache server threads")
            public void shouldStartCacheServerThreads() {
                int num = 5;
                cacheServerManager.startupCacheServer(num);
                verify(mockFactory, times(num)).produceCacheServerThread(any(CacheServer.class));
            }

            @Test
            @DisplayName("should notify ServerMonitorRunnable that there is a new server")
            public void serverMonitorRunnableShouldBeUpdated() {
                int num = 5;
                cacheServerManager.startupCacheServer(num);
                Map<Integer, ServerInfo> info = cacheServerManager.serverMonitor.getServerInfo();
                assertEquals(num, info.size());
            }
        }

        @Nested
        @DisplayName("Testing shutdownCacheServer()")
        public class TestingShutdownCacheServer {
            @BeforeEach
            public void setup() {
                cacheServerManager.startupCacheServer(10);
            }

            @Test
            @DisplayName("should shutdown cache server threads")
            public void shouldShutDownCacheServerInstance() {
                int num = 5;
                cacheServerManager.shutdownCacheServer(num);

            }

            @Test
            @DisplayName("should remove shutdown cache servers from serverThreadTable")
            public void shouldRemoveShutdownCacheServers() {

            }

            @Test
            @DisplayName("should notify ServerMonitorRunnable that a server has been removed")
            public void shouldNotifyServerMonitorRunnable() {

            }
        }

        @Test
        @DisplayName("When CacheServerMonitor thread is interrupted, it interrupts all cache servers that it has spawned")
        public void cacheServerMonitorThreadInterruptedInterruptsAllCacheServers() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost req = new HttpPost("http://127.0.0.1:" + cacheServerMonitorPort + "/cache-servers");

            // send request to server and wait for it to be received
            try {
                CloseableHttpResponse response = client.execute(req);
                client.close();
                Thread.sleep(100);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // interrupt cacheServerMonitor thread
            cacheServerMonitorThread.interrupt();

            // wait for CacheServerMonitor to run interruption callbacks
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }

            // verify that CacheServerThread has been interrupted
            verify(mockCacheServerThread, times(1)).interrupt();
        }
    }

    @Nested
    @DisplayName("Testing with a live cache server")
    public class LiveCacheServerTests {
        CacheServerFactory factory;
        int cacheServerMonitorPort;
        int cacheServerPort;

        @BeforeEach
        public void setup() {
            factory = new CacheServerFactoryImpl();
            cacheServerManager = new CacheServerManager(factory, new HttpClientFactoryImpl(), new RequestDecoderImpl());
            cacheServerMonitorThread = new Thread(cacheServerManager);
            cacheServerMonitorThread.start();
            cacheServerMonitorPort = CacheServerManagerTest.waitUntilServerReady(cacheServerManager);
            startServerAndGetPort();
        }

        private void startServerAndGetPort() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost req = new HttpPost("http://127.0.0.1:" + cacheServerMonitorPort + "/cache-servers");
            cacheServerPort = -1;

            try {
                CloseableHttpResponse response = client.execute(req);
                RequestDecoder decoder = new RequestDecoderImpl();
                JSONObject jsonObj = decoder.extractJsonApacheResponse(response);
                int portInt = jsonObj.getInt("port");
                HttpEntity responseBody = response.getEntity();
                InputStream responseStream = responseBody.getContent();
                String responseString = IOUtils.toString(responseStream, StandardCharsets.UTF_8.name());
                response.close();
                responseStream.close();
                cacheServerPort = portInt;
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        @Test
//        @DisplayName("When a new cache server is started, it should return the port that the new cache server is running on")
//        public void cacheServerStartedReturnPort() {
//            // send request to port returned by the request to see if the server is active
//            CloseableHttpClient client = HttpClients.createDefault();
//            int status = -1;
//
//            if (cacheServerPort != -1) {
//                try {
//                    HttpGet getRequest = new HttpGet("http://127.0.0.1:" + cacheServerPort);
//                    CloseableHttpResponse response = client.execute(getRequest);
//                    status = response.getStatusLine().getStatusCode();
//                } catch (IOException e) { e.printStackTrace(); }
//            }
//
//            cacheServerMonitorThread.interrupt();
//
//            assertEquals(status, 200);
//        }
    }

    // waits until a server has started up
    // returns port
    private static int waitUntilServerReady(CacheServerManager cacheServerManager) {
        int cacheServerMonitorPort = cacheServerManager.getPort();

        while (cacheServerMonitorPort == -1) {
            try {
                Thread.sleep(20);
                cacheServerMonitorPort = cacheServerManager.getPort();
            } catch (InterruptedException e) { }
        }

        return cacheServerMonitorPort;
    }
}