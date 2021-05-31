package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoder;


import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

public class ServerMonitorRunnableTest {
    CacheServerManager mockCacheServerManager;
    HttpClientFactory clientFactory;
    RequestDecoder mockDecoder;
    ServerMonitorImpl mockServerMonitor;
    ServerMonitorRunnable serverMonitorRunnable;
    Thread serverMonitorThread;

    @BeforeAll
    public static void config() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.THREAD_MANAGEMENT });
    }

    @Nested
    @DisplayName("Tests tick()")
    class TestRun {
        int numServers = 10;

        @BeforeEach
        public void setup() {
            mockServerMonitor = Mockito.mock(ServerMonitorImpl.class);
            clientFactory = Mockito.mock(HttpClientFactory.class);
            mockDecoder = Mockito.mock(RequestDecoder.class);
            mockCacheServerManager = Mockito.mock(CacheServerManager.class);
            when(mockCacheServerManager.numServers()).thenReturn(numServers);

            serverMonitorRunnable = new ServerMonitorRunnable(mockServerMonitor, mockCacheServerManager);
            serverMonitorThread = new Thread(serverMonitorRunnable);
            serverMonitorThread.start();
        }

        @AfterEach
        public void cleanup() {
            serverMonitorThread.interrupt();
        }

        @Test
        @DisplayName("ServerMonitor should run updateServerCount method")
        public void shouldRunUpdateServerCount() throws InterruptedException {
            Thread.sleep(500);
            verify(mockServerMonitor, atLeast(1)).updateServerCount(anyInt(), eq(numServers));
        }
    }
}