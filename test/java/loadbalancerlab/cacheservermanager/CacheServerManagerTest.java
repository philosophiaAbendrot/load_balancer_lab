package loadbalancerlab.cacheservermanager;

import loadbalancerlab.util.RequestDecoder;
import loadbalancerlab.util.RequestDecoderImpl;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.apache.http.impl.client.HttpClients;
import org.mockito.Mockito;

import loadbalancerlab.factory.CacheServerFactoryImpl;
import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.services.monitor.RequestMonitor;
import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.factory.HttpClientFactoryImpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CacheServerManagerTest {
    @Nested
    @DisplayName("Testing with a mock cache server")
    public class MockCacheServerTests {
        CacheServerFactory mockFactory;
        CacheServer mockCacheServer;
        Thread mockCacheServerThread;
        CacheServerManager cacheServerManager;
        Thread cacheServerMonitorThread;
        int cacheServerMonitorPort;

        @BeforeEach
        public void setup() {
            this.mockFactory = Mockito.mock(CacheServerFactoryImpl.class);
            this.mockCacheServer = Mockito.mock(CacheServer.class);
            this.mockCacheServer.port = 37_100;
            this.mockCacheServerThread = Mockito.mock(Thread.class);

            when(this.mockFactory.produceCacheServer(any(RequestMonitor.class))).thenReturn(mockCacheServer);
            when(this.mockFactory.produceCacheServerThread(any(CacheServer.class))).thenReturn(this.mockCacheServerThread);

            this.cacheServerManager = new CacheServerManager(this.mockFactory, new HttpClientFactoryImpl(), new RequestDecoderImpl());
            this.cacheServerMonitorThread = new Thread(this.cacheServerManager);
            this.cacheServerMonitorThread.start();
            this.cacheServerMonitorPort = CacheServerManagerTest.waitUntilServerReady(this.cacheServerManager);
        }

        @Test
        @DisplayName("CacheServerMonitor should start up a CacheServer instance when sent a request telling it to do so")
        public void CacheServerMonitorShouldStartCacheServerInstance() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost req = new HttpPost("http://127.0.0.1:" + this.cacheServerMonitorPort + "/cache-servers");

            try {
                CloseableHttpResponse response = client.execute(req);
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.cacheServerMonitorThread.interrupt();

            verify(this.mockFactory, times(1)).produceCacheServer(any(RequestMonitor.class));
            verify(this.mockFactory, times(1)).produceCacheServerThread(any(CacheServer.class));
        }

        @Test
        @DisplayName("When CacheServerMonitor thread is interrupted, it interrupts all cache servers that it has spawned")
        public void CacheServerMonitorThreadInterruptedInterruptsAllCacheServers() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost req = new HttpPost("http://127.0.0.1:" + this.cacheServerMonitorPort + "/cache-servers");

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
            this.cacheServerMonitorThread.interrupt();

            // wait for CacheServerMonitor to run interruption callbacks
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }

            // verify that CacheServerThread has been interrupted
            verify(this.mockCacheServerThread, times(1)).interrupt();
        }
    }

    @Nested
    @DisplayName("Testing with a live cache server")
    public class LiveCacheServerTests {
        CacheServerFactory factory;
        CacheServerManager cacheServerManager;
        Thread cacheServerMonitorThread;
        int cacheServerMonitorPort;
        int cacheServerPort;

        @BeforeEach
        public void setup() {
            this.factory = new CacheServerFactoryImpl();
            this.cacheServerManager = new CacheServerManager(this.factory, new HttpClientFactoryImpl(), new RequestDecoderImpl());
            this.cacheServerMonitorThread = new Thread(this.cacheServerManager);
            this.cacheServerMonitorThread.start();
            this.cacheServerMonitorPort = CacheServerManagerTest.waitUntilServerReady(this.cacheServerManager);
            startServerAndGetPort();
        }

        private void startServerAndGetPort() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost req = new HttpPost("http://127.0.0.1:" + this.cacheServerMonitorPort + "/cache-servers");
            this.cacheServerPort = -1;

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
                this.cacheServerPort = portInt;
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Test
        @DisplayName("When a new cache server is started, it should return the port that the new cache server is running on")
        public void cacheServerStartedReturnPort() {
            // send request to port returned by the request to see if the server is active
            CloseableHttpClient client = HttpClients.createDefault();
            int status = -1;

            if (this.cacheServerPort != -1) {
                try {
                    HttpGet getRequest = new HttpGet("http://127.0.0.1:" + this.cacheServerPort);
                    CloseableHttpResponse response = client.execute(getRequest);
                    status = response.getStatusLine().getStatusCode();
                } catch (IOException e) { e.printStackTrace(); }
            }

            this.cacheServerMonitorThread.interrupt();

            assertEquals(status, 200);
        }

        @Test
        @DisplayName("When a request is sent to shut down a certain server, that server should be shut down")
        public void shutDownSpecificServer() {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpDelete req = new HttpDelete("http://127.0.0.1:" + this.cacheServerMonitorPort + "/cache-server/" + this.cacheServerPort);

            try {
                CloseableHttpResponse response = client.execute(req);
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            HttpGet getReq = new HttpGet("http://127.0.0.1:" + this.cacheServerPort);
            final CloseableHttpClient client2 = HttpClients.createDefault();

            Exception exception = assertThrows(IOException.class, () -> {
               CloseableHttpResponse response = client2.execute(getReq);
            });
        }
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