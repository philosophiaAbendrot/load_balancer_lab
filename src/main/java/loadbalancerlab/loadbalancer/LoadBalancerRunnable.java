package loadbalancerlab.loadbalancer;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class LoadBalancerRunnable implements Runnable {
    private int port;
    private int cacheServerManagerPort;
    private Thread clientReqHandlerThread;
    private ClientRequestHandlerServer clientReqHandlerRunnable;
    private LoadBalancerClientRequestHandler clientReqHandler;
    private CacheRedistributor cacheRedis;
    private Thread cacheRedisThread;
    private HashRing hashRing;

    public LoadBalancerRunnable(int _cacheServerManagerPort) {
        cacheServerManagerPort = _cacheServerManagerPort;

        // prepare CacheRedistributor
        hashRing = new HashRing();
        cacheRedis = new CacheRedistributor(cacheServerManagerPort, hashRing);
        CacheRedistributorRunnable cacheRedisRunnable = new CacheRedistributorRunnable(cacheRedis);
        cacheRedisThread = new Thread(cacheRedisRunnable);

        // prepare ClientRequestHandler server
        clientReqHandler = new LoadBalancerClientRequestHandler(cacheRedis);
        clientReqHandlerRunnable = new ClientRequestHandlerServer(clientReqHandler);
        clientReqHandlerThread = new Thread(clientReqHandlerRunnable);
    }

    public SortedMap<Integer, Map<Integer, List<HashRingAngle>>> getHashRingAngleHistory() {
        return cacheRedis.getHashRingAngleHistory();
    }

    @Override
    public void run() {
        // startup cache redistributor and client request handler
        cacheRedisThread.start();
        clientReqHandlerThread.start();

        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
                break;
            }
        }

        // shutdown sub-threads
        cacheRedisThread.interrupt();
        clientReqHandlerThread.interrupt();
    }

    public int getPort() {
        return clientReqHandlerRunnable.getPort();
    }
}