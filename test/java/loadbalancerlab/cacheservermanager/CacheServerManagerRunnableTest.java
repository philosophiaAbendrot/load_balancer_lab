package loadbalancerlab.cacheservermanager;

import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.factory.CacheServerFactoryImpl;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.services.monitor.RequestMonitor;
import loadbalancerlab.shared.RequestDecoderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class CacheServerManagerRunnableTest {
    CacheServerManagerConfig config;
    int cacheServerManagerPort = 8_000;
    int sleepInterval = 100;
    int cacheServerPort = 1_000;
    CacheServerManager cacheServerManager;
    CacheServerManagerRunnable cacheServerManagerRunnable;
    Thread cacheServerManagerThread;
    CacheServerFactoryImpl mockCacheServerFactory;
    Thread mockCacheServerThread;
    CacheServer mockCacheServer;

    @BeforeEach
    public void setup() {
        mockCacheServerFactory = Mockito.mock(CacheServerFactoryImpl.class);
        mockCacheServerThread = Mockito.mock(Thread.class);
        mockCacheServer = Mockito.mock(CacheServer.class);
        when(mockCacheServer.getPort()).thenReturn(cacheServerPort);
        when(mockCacheServerFactory.produceCacheServer(any(RequestMonitor.class))).thenReturn(mockCacheServer);
        when(mockCacheServerFactory.produceCacheServerThread(any(CacheServer.class))).thenReturn(mockCacheServerThread);

        cacheServerManager = new CacheServerManager(mockCacheServerFactory, new HttpClientFactoryImpl(), new RequestDecoderImpl());
        config = new CacheServerManagerConfig(cacheServerManager, cacheServerManagerPort, sleepInterval);
        cacheServerManagerRunnable = new CacheServerManagerRunnable(config);
        cacheServerManagerThread = new Thread(cacheServerManagerRunnable);
        cacheServerManagerThread.start();
    }

    @Test
    @DisplayName("When CacheServerMonitor thread is interrupted, it interrupts all cache servers that it has spawned")
    public void cacheServerMonitorThreadInterruptedInterruptsAllCacheServers() {
        // interrupt cacheServerMonitor thread
        cacheServerManager.startupCacheServer(1);
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