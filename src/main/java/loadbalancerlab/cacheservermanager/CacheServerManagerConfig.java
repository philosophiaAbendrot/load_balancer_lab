package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheInfoServerFactory;
import loadbalancerlab.factory.ServerMonitorFactory;

// takes CacheServerManager instance as input and handles
// setup of instances necessary for CacheServerManagerRunnable
public class CacheServerManagerConfig {
    public CacheServerManager cacheServerManager;
    public ServerMonitor serverMonitor;
    public ServerMonitorRunnable serverMonitorRunnable;
    public Thread serverMonitorThread;
    public CacheInfoRequestHandler cacheInfoRequestHandler;
    public CacheInfoServerRunnable cacheInfoServerRunnable;
    public Thread cacheInfoServerThread;
    public int sleepInterval;

    public CacheServerManagerConfig(CacheServerManager _cacheServerManager, int cacheInfoServerPort, int _sleepInterval) {
        cacheServerManager = _cacheServerManager;
        ServerMonitorFactory serverMonitorFactory = new ServerMonitorFactory();
        CacheInfoServerFactory cacheInfoServerFactory = new CacheInfoServerFactory();

        serverMonitor = serverMonitorFactory.produceServerMonitor(cacheServerManager);
        serverMonitorRunnable = serverMonitorFactory.produceServerMonitorRunnable(serverMonitor, cacheServerManager);
        serverMonitorThread = new Thread(serverMonitorRunnable);
        cacheInfoRequestHandler = cacheInfoServerFactory.produceCacheInfoRequestHandler(serverMonitor);
        cacheInfoServerRunnable = cacheInfoServerFactory.produceCacheInfoServerRunnable(cacheInfoRequestHandler);
        cacheInfoServerThread = new Thread(cacheInfoServerRunnable);

        sleepInterval = _sleepInterval;
    }
}