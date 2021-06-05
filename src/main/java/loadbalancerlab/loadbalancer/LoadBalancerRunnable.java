package loadbalancerlab.loadbalancer;

public class LoadBalancerRunnable implements Runnable {
    private int port;
    private int cacheServerManagerPort;
    private Thread clientReqHandlerServer;
    private ClientRequestHandlerServer clientReqHandlerRunnable;
    private ClientRequestHandler clientReqHandler;
    private CacheRedistributor cacheRedisImpl;
    private Thread cacheRedisThread;
    private HashRing hashRing;

    public LoadBalancerRunnable(int _cacheServerManagerPort) {
        cacheServerManagerPort = _cacheServerManagerPort;

        // prepare CacheRedistributor
        hashRing = new HashRing();
        cacheRedisImpl = new CacheRedistributor(cacheServerManagerPort, hashRing);
        CacheRedistributorRunnable cacheRedisRunnable = new CacheRedistributorRunnable(cacheRedisImpl);
        cacheRedisThread = new Thread(cacheRedisRunnable);

        // prepare ClientRequestHandler server
        clientReqHandler = new ClientRequestHandler(cacheRedisImpl);
        clientReqHandlerRunnable = new ClientRequestHandlerServer(clientReqHandler);
        clientReqHandlerServer = new Thread(clientReqHandlerRunnable);
    }

    @Override
    public void run() {
        // startup cache redistributor and client request handler
        cacheRedisThread.start();
        clientReqHandlerServer.start();

        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // shutdown sub-threads
        cacheRedisThread.interrupt();
        clientReqHandlerServer.interrupt();
    }

    public int getPort() {
        return clientReqHandlerRunnable.getPort();
    }
}