package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactoryImpl;
import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.services.monitor.RequestMonitor;
import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoderImpl;

import loadbalancerlab.shared.ServerInfo;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CacheServerManagerTest {
    CacheServerManager cacheServerManager;
    Thread cacheServerManagerThread;
    CacheServerFactory mockFactory;
    CacheServer mockCacheServer;
    Thread mockCacheServerThread;
    int cacheServerMonitorPort;

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
    }

    @BeforeEach
    public void setup() {
        mockFactory = Mockito.mock(CacheServerFactoryImpl.class);
        mockCacheServer = Mockito.mock(CacheServer.class);
        mockCacheServer.port = 37_100;
        mockCacheServerThread = Mockito.mock(Thread.class);

        when(mockFactory.produceCacheServer(any(RequestMonitor.class))).thenReturn(mockCacheServer);
        when(mockFactory.produceCacheServerThread(any(CacheServer.class))).thenReturn(mockCacheServerThread);

        cacheServerManager = new CacheServerManager(mockFactory, new HttpClientFactoryImpl(), new RequestDecoderImpl());
        cacheServerManagerThread = new Thread(cacheServerManager);
        cacheServerManagerThread.start();
        cacheServerMonitorPort = CacheServerManagerTest.waitUntilServerReady(cacheServerManager);
    }

    @AfterEach
    public void shutdown() {
        cacheServerManagerThread.interrupt();
    }

    @Nested
    @DisplayName("Testing startupCacheServer()")
    public class TestingStartupCacheServer {
        int num = 5;

        @Test
        @DisplayName("should startup cache servers")
        public void shouldStartCacheServerInstances() {
            cacheServerManager.startupCacheServer(num);
            verify(mockFactory, times(num)).produceCacheServer(any(RequestMonitor.class));
        }

        @Test
        @DisplayName("should startup cache server threads")
        public void shouldStartCacheServerThreads() {
            cacheServerManager.startupCacheServer(num);
            verify(mockFactory, times(num)).produceCacheServerThread(any(CacheServer.class));
        }

        @Test
        @DisplayName("server monitor runnable should be updated")
        public void serverMonitorRunnableShouldBeUpdated() {
            cacheServerManager.startupCacheServer(num);
            Map<Integer, ServerInfo> info = cacheServerManager.serverMonitor.getServerInfo();
            assertEquals(num, info.size());
        }
    }

    @Nested
    @DisplayName("Testing shutdownCacheServer()")
    public class TestingShutdownCacheServer {
        int num = 5;

        @BeforeEach
        public void setup() {
            cacheServerManager.startupCacheServer(num);
        }

        @Test
        @DisplayName("should shutdown all cache server threads")
        public void shouldShutDownAllCacheServerInstances() throws InterruptedException {
            cacheServerManager.shutdownCacheServer(num);

            for (Thread serverThread : new ArrayList<>(cacheServerManager.serverThreadTable.values())) {
                assertEquals(true, serverThread.isInterrupted());
            }
        }

        @Test
        @DisplayName("should remove shutdown cache servers from serverThreadTable")
        public void shouldRemoveShutdownCacheServers() {
            cacheServerManager.shutdownCacheServer(num);
            assertEquals(0, cacheServerManager.serverThreadTable.size());
        }

        @Test
        @DisplayName("should update server monitor runnable")
        public void serverMonitorRunnableShouldBeUpdated() {
            cacheServerManager.shutdownCacheServer(num);
            assertEquals(0, cacheServerManager.serverMonitor    .serverInfoTable.size());
        }

        @Nested
        @DisplayName("When shutting donw more servers than exists")
        public class WhenShuttingDownMoreServersThanExists {
            int numShutdown = 8;

            @Test
            @DisplayName("should shutdown all cache server threads")
            public void shouldShutDownAllCacheServerInstances() throws InterruptedException {
                cacheServerManager.shutdownCacheServer(numShutdown);

                for (Thread serverThread : new ArrayList<>(cacheServerManager.serverThreadTable.values())) {
                    assertEquals(true, serverThread.isInterrupted());
                }
            }

            @Test
            @DisplayName("should remove shutdown cache servers from serverThreadTable")
            public void shouldRemoveShutdownCacheServers() {
                cacheServerManager.shutdownCacheServer(numShutdown);
                assertEquals(0, cacheServerManager.serverThreadTable.size());
            }

            @Test
            @DisplayName("should update server monitor runnable")
            public void serverMonitorRunnableShouldBeUpdated() {
                cacheServerManager.shutdownCacheServer(numShutdown);
                assertEquals(0, cacheServerManager.serverMonitor.serverInfoTable.size());
            }
        }

        @Nested
        @DisplayName("When shutting down less servers than exists")
        public class WhenShuttingDownLessServersThanExists {
            int numShutdown = 3;

            @Test
            @DisplayName("should shutdown the correct number of server threads")
            public void shouldShutdownCorrectNumberOfServers() throws InterruptedException {
                cacheServerManager.shutdownCacheServer(numShutdown);

                int numLiveServers = 0;

                for (Thread serverThread : new ArrayList<>(cacheServerManager.serverThreadTable.values())) {
                    if (!serverThread.isInterrupted())
                        numLiveServers++;
                }
                assertEquals(num - numShutdown, numLiveServers);
            }

            @Test
            @DisplayName("should remove correct number of cache servers from server thread table")
            public void shouldRemoveCorrectNumberOfThreads() {
                cacheServerManager.shutdownCacheServer(numShutdown);
                assertEquals(num - numShutdown, cacheServerManager.serverThreadTable.size());
            }

            @Test
            @DisplayName("should remove correct number of entries in ServerMonitor server info table")
            public void serverMonitorRunnableShouldBeUpdated() {
                cacheServerManager.shutdownCacheServer(numShutdown);
                assertEquals(num - numShutdown, cacheServerManager.serverMonitor.serverInfoTable.size());
            }
        }
    }

    @Test
    @DisplayName("When CacheServerMonitor thread is interrupted, it interrupts all cache servers that it has spawned")
    public void cacheServerMonitorThreadInterruptedInterruptsAllCacheServers() {
        // interrupt cacheServerMonitor thread
        cacheServerManager.startupCacheServer(1);
        cacheServerManagerThread.interrupt();
        // wait for CacheServerMonitor to run interruption callbacks
        try {
            Thread.sleep(3_000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        verify(mockCacheServerThread, times(1)).interrupt();
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