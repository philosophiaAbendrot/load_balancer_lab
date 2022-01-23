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
import java.util.Set;
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

        /* Configure logger to print nothing */
        Logger.setPrintSetting(-1);
    }

    @BeforeEach
    public void setup() {
        mockFactory = Mockito.mock(CacheServerFactory.class);
        mockCacheServer = Mockito.mock(CacheServer.class);
        when(mockCacheServer.getPort()).thenReturn(37_100);
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
            Set<Integer> keySet = cacheServerManager.serverMonitor.serverInfoTable.keySet();
            Integer key = new ArrayList<>(keySet).get(0);
            assertEquals(false, cacheServerManager.serverMonitor.serverInfoTable.get(key).getActive());
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
                Set<Integer> keySet = cacheServerManager.serverMonitor.serverInfoTable.keySet();
                Integer key = new ArrayList<>(keySet).get(0);
                assertEquals(false, cacheServerManager.serverMonitor.serverInfoTable.get(key).getActive());
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
            @DisplayName("should remove correct number of active entries in ServerMonitor server info table")
            public void serverMonitorRunnableShouldBeUpdated() {
                cacheServerManager.shutdownCacheServer(numShutdown);

                int numActiveServers = 0;

                for (ServerInfo info : cacheServerManager.serverMonitor.serverInfoTable.values()) {
                    if (info.getActive())
                        numActiveServers++;
                }

                assertEquals(num - numShutdown, numActiveServers);
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
            double averageCf;
            double diff = 0.2;

            @BeforeEach
            public void setup() {
                cacheServerManager.serverThreadTable = new ConcurrentHashMap<>();
                averageCf = config.getTargetCf() + diff;

                for (int i = 0; i < initialServerCount; i++) {
                    Thread mockThread = Mockito.mock(Thread.class);
                    cacheServerManager.serverThreadTable.put(i, mockThread);
                }

                CacheServerManager.cacheServerIdCounter = initialServerCount;
                mockServerMonitor = Mockito.mock(ServerMonitor.class);
                when(mockServerMonitor.getAverageCf()).thenReturn(averageCf);
                cacheServerManager.serverMonitor = mockServerMonitor;
                cacheServerManager.modulateCapacity();
            }

            @Test
            @DisplayName("number of servers should be increased")
            public void numServersShouldIncrease() {
                double cacheServerGrowthRate = config.getCacheServerGrowthRate();
                int intDiff = (int)Math.round(diff * (cacheServerGrowthRate / 100) * initialServerCount);
                int expectedServers = initialServerCount + intDiff;
                assertEquals(expectedServers, cacheServerManager.serverThreadTable.size());
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
            double averageCf;
            double diff = -0.2;

            @BeforeEach
            public void setup() {
                cacheServerManager.serverThreadTable = new ConcurrentHashMap<>();
                averageCf = config.getTargetCf() + diff;

                for (int i = 0; i < initialServerCount; i++) {
                    Thread mockThread = Mockito.mock(Thread.class);
                    cacheServerManager.serverThreadTable.put(i, mockThread);
                }

                CacheServerManager.cacheServerIdCounter = initialServerCount;
                mockServerMonitor = Mockito.mock(ServerMonitor.class);

                when(mockServerMonitor.getAverageCf()).thenReturn(averageCf);
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