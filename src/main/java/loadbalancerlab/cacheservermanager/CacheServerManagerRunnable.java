package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.RequestDecoder;

public class CacheServerManagerRunnable implements Runnable {
    CacheServerManager cacheServerManager;
    ServerMonitor serverMonitor;
    CacheInfoRequestHandler cacheInfoRequestHandler;

    ServerMonitorRunnable serverMonitorRunnable;
    CacheInfoServerRunnable cacheInfoServerRunnable;

    Thread serverMonitorThread;
    Thread cacheInfoServerThread;
    static int sleepInterval = 50;

    RequestDecoder reqDecoder;
    CacheServerFactory cacheServerFactory;
    HttpClientFactory clientFactory;

    public CacheServerManagerRunnable( CacheServerFactory _cacheServerFactory, HttpClientFactory _clientFactory, RequestDecoder _reqDecoder, CacheServerManager _cacheServerManager ) {
        reqDecoder = _reqDecoder;
        clientFactory = _clientFactory;
        cacheServerFactory = _cacheServerFactory;

        cacheServerManager = _cacheServerManager;

        // generate instances of sub-components
        serverMonitor = new ServerMonitor(clientFactory, reqDecoder, cacheServerManager);
        cacheInfoRequestHandler = new CacheInfoRequestHandler(serverMonitor);

        // generate runnables for sub-components
        serverMonitorRunnable = new ServerMonitorRunnable(serverMonitor, cacheServerManager);
        cacheInfoServerRunnable = new CacheInfoServerRunnable(cacheInfoRequestHandler);

        // generate threads for sub-components
        serverMonitorThread = new Thread(serverMonitorRunnable);
        cacheInfoServerThread = new Thread(cacheInfoServerRunnable);
    }

    public int getPort() {
        return cacheInfoServerRunnable.getPort();
    }

    @Override
    public void run() {
        // start sub-threads
        serverMonitorThread.start();
        cacheInfoServerThread.start();

        while (true) {
            try {
                cacheServerManager.modulateCapacity();
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // interrupt sub-threads
        serverMonitorThread.interrupt();
        cacheInfoServerThread.interrupt();

        // interrupt cache server threads
        for (Thread serverThread : cacheServerManager.serverThreadTable.values()) {
            serverThread.interrupt();
        }
    }
}