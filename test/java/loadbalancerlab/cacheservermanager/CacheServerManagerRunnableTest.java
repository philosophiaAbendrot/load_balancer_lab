package loadbalancerlab.cacheservermanager;

import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.factory.CacheInfoServerFactory;
import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.cacheserver.RequestMonitor;
import loadbalancerlab.factory.ServerMonitorFactory;
import loadbalancerlab.shared.RequestDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class CacheServerManagerRunnableTest {
    int cacheServerManagerPort = 8_000;
    int sleepInterval = 100;
    int cacheServerPort = 1_000;
    CacheServerManager cacheServerManager;
    CacheServerManagerRunnable cacheServerManagerRunnable;
    Thread cacheServerManagerThread;
    CacheServerFactory mockCacheServerFactory;
    Thread mockCacheServerThread;
    CacheServer mockCacheServer;
    CacheInfoServerFactory cacheInfoServerFactory;

    @BeforeEach
    public void setup() {
        mockCacheServerFactory = Mockito.mock(CacheServerFactory.class);
        mockCacheServerThread = Mockito.mock(Thread.class);
        mockCacheServer = Mockito.mock(CacheServer.class);
        cacheInfoServerFactory = new CacheInfoServerFactory();
        when(mockCacheServer.getPort()).thenReturn(cacheServerPort);
        when(mockCacheServerFactory.produceCacheServer(any(RequestMonitor.class))).thenReturn(mockCacheServer);
        when(mockCacheServerFactory.produceCacheServerThread(any(CacheServer.class))).thenReturn(mockCacheServerThread);

        cacheServerManager = new CacheServerManager(mockCacheServerFactory, new HttpClientFactory(),
                                                    new RequestDecoder(), cacheInfoServerFactory, new ServerMonitorFactory());
        cacheServerManagerRunnable = new CacheServerManagerRunnable(mockCacheServerFactory, new HttpClientFactory(),
                                                                    new RequestDecoder(), cacheServerManager,
                                                                    cacheInfoServerFactory);
        cacheServerManagerThread = new Thread(cacheServerManagerRunnable);
        cacheServerManagerThread.start();
    }

    @Test
    @DisplayName("When CacheServerMonitor thread is interrupted, it interrupts all cache servers that it has spawned")
    public void cacheServerMonitorThreadInterruptedInterruptsAllCacheServers() {
        cacheServerManager.startupCacheServer(1);

        // wait for CacheServerMonitor to run interruption callbacks
        try {
            Thread.sleep(500);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        // interrupt cacheServerMonitor thread
        cacheServerManagerThread.interrupt();

        // wait for CacheServerMonitor to run interruption callbacks
        try {
            Thread.sleep(500);
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