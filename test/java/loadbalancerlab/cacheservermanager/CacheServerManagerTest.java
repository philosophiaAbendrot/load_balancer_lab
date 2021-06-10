package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.cacheserver.RequestMonitor;
import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoder;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CacheServerManagerTest {
    CacheServerManager cacheServerManager;
    CacheServerFactory mockFactory;
    CacheServer mockCacheServer;
    Thread mockCacheServerThread;

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
    }

    @BeforeEach
    public void setup() {
        mockFactory = Mockito.mock(CacheServerFactory.class);
        mockCacheServer = Mockito.mock(CacheServer.class);
        mockCacheServer.setPort((int)Math.round(Math.random() * 50_000));
        mockCacheServerThread = Mockito.mock(Thread.class);

        when(mockFactory.produceCacheServer(any(RequestMonitor.class))).thenReturn(mockCacheServer);
        when(mockFactory.produceCacheServerThread(any(CacheServer.class))).thenReturn(mockCacheServerThread);

        cacheServerManager = new CacheServerManager(mockFactory, new HttpClientFactory(), new RequestDecoder());
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
            assertEquals(0, cacheServerManager.serverMonitor.serverInfoTable.size());
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

    @Nested
    @DisplayName("Testing modulateCapacity()")
    class ModulateCapacity {
        ServerMonitor mockServerMonitor;
        int initialServerCount = 100;
        Config config;

        @BeforeEach
        public void setup() {
            config = new Config();
            CacheServerManager.configure(config);
            cacheServerManager = new CacheServerManager(mockFactory, new HttpClientFactory(), new RequestDecoder());
        }

        @Nested
        @DisplayName("when average capacity factor exceeds target by 20%")
        class WhenAverageCapacityFactorExceedsThreshold {
            double targetCf;

            @BeforeEach
            public void setup() {
                System.out.println("path 1");
                cacheServerManager.serverThreadTable = new ConcurrentHashMap<>();
                System.out.println("path 2");
                targetCf = config.getTargetCf() + 0.2f;
                System.out.println("path 3");

                for (int i = 0; i < initialServerCount; i++) {
                    System.out.println("path 4");
                    Thread mockThread = Mockito.mock(Thread.class);
                    cacheServerManager.serverThreadTable.put(i, mockThread);
                }
                System.out.println("path 5");

                CacheServerManager.cacheServerIdCounter = initialServerCount;

                System.out.println("path 6");
                mockServerMonitor = Mockito.mock(ServerMonitor.class);

                System.out.println("path 7");
                when(mockServerMonitor.getAverageCf()).thenReturn(targetCf);
                System.out.println("path 8");
                cacheServerManager.serverMonitor = mockServerMonitor;
                System.out.println("path 9");
                cacheServerManager.modulateCapacity();
                System.out.println("path 10");
            }

            @Test
            @DisplayName("number of servers should be increased by 5%")
            public void numServersShouldIncrease() {
                assertEquals(Math.round(initialServerCount * 1.05), cacheServerManager.serverThreadTable.size());
            }
        }

        @Nested
        @DisplayName("when average capacity factor equals the target")
        class WhenAverageCapacityFactorEqualsTarget {
            double targetCf;

            @BeforeEach
            public void setup() {
                cacheServerManager.serverThreadTable = new ConcurrentHashMap<>();
                targetCf = config.getTargetCf();

                for (int i = 0; i < initialServerCount; i++) {
                    Thread mockThread = Mockito.mock(Thread.class);
                    cacheServerManager.serverThreadTable.put(i, mockThread);
                }

                CacheServerManager.cacheServerIdCounter = initialServerCount;
                mockServerMonitor = Mockito.mock(ServerMonitor.class);

                when(mockServerMonitor.getAverageCf()).thenReturn(targetCf);
                cacheServerManager.serverMonitor = mockServerMonitor;
                cacheServerManager.modulateCapacity();
            }

            @Test
            @DisplayName("number of servers should remain the same")
            public void numServersShouldRemainSame() {
                assertEquals(Math.round(initialServerCount), cacheServerManager.serverThreadTable.size());
            }
        }

        @Nested
        @DisplayName("when average capacity factor is lower than the target by 20%")
        class WhenAverageCapacityFactorIsBelowThreshold {
            double currentCf;
            double diff = -0.2;

            @BeforeEach
            public void setup() {
                cacheServerManager.serverThreadTable = new ConcurrentHashMap<>();
                currentCf = config.getTargetCf() + diff;

                for (int i = 0; i < initialServerCount; i++) {
                    Thread mockThread = Mockito.mock(Thread.class);
                    cacheServerManager.serverThreadTable.put(i, mockThread);
                }

                CacheServerManager.cacheServerIdCounter = initialServerCount;
                mockServerMonitor = Mockito.mock(ServerMonitor.class);

                when(mockServerMonitor.getAverageCf()).thenReturn(currentCf);
                cacheServerManager.serverMonitor = mockServerMonitor;
                cacheServerManager.modulateCapacity();
            }

            @Test
            @DisplayName("number of servers should decrease by 5%")
            public void numServersShouldDecrease() {
                double cacheServerGrowthRate = config.getCacheServerGrowthRate();
                int intDiff = (int)Math.round(diff * (cacheServerGrowthRate / 100) * initialServerCount);
                int expectedServers = initialServerCount + intDiff;
                assertEquals(expectedServers, cacheServerManager.serverThreadTable.size());
            }
        }
    }
}