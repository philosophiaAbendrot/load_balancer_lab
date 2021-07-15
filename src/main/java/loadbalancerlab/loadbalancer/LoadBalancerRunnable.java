package loadbalancerlab.loadbalancer;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * LoadBalancer object which handles delegation and forwarding of incoming client Http requests to CacheServer objects.
 */
public class LoadBalancerRunnable implements Runnable {

    /**
     * Port which associated CacheServerManager object is running on.
     */
    private int cacheServerManagerPort;

    /**
     * A Thread object which runs a server which handles Http requests from clients.
     * Runs the 'clientReqHandlerRunnable' object.
     */
    private Thread clientReqHandlerThread;

    /**
     * Server which handles Http requests from clients.
     */
    private ClientRequestHandlerServer clientReqHandlerRunnable;

    /**
     * LoadBalancerClientRequestHandler object which contains the logic to handle Http requests from clients.
     * Inserted into 'clientReqHandlerRunnable' object upon creation.
     */
    private LoadBalancerClientRequestHandler clientReqHandler;

    /**
     * CacheRedistributor object which manages the dynamic-redistribution of resource names among CacheServer objects.
     * Acts as a middleman between this object and a HashRing object which handles a consistent hashing mechanism.
     */
    private CacheRedistributor cacheRedis;

    /**
     * HashRing object which handles consistent hashing mechanism for mapping resource names to CacheServers.
     */
    private HashRing hashRing;

    /**
     * Thread instance which runs 'cacheRedis' object.
     */
    private Thread cacheRedisThread;

    /**
     * @param cacheServerManagerPort        Port which associated CacheServerManager object is running on.
     */
    public LoadBalancerRunnable( int cacheServerManagerPort ) {
        this.cacheServerManagerPort = cacheServerManagerPort;

        /* Setup CacheRedistributor */
        hashRing = new HashRing();
        cacheRedis = new CacheRedistributor(this.cacheServerManagerPort, hashRing);
        CacheRedistributorRunnable cacheRedisRunnable = new CacheRedistributorRunnable(cacheRedis);
        cacheRedisThread = new Thread(cacheRedisRunnable);

        /* Setup ClientRequestHandler server */
        clientReqHandler = new LoadBalancerClientRequestHandler(cacheRedis);
        clientReqHandlerRunnable = new ClientRequestHandlerServer(clientReqHandler);
        clientReqHandlerThread = new Thread(clientReqHandlerRunnable);
    }

    /**
     * Getter method for CacheRedistributor.angleHistory field, which stores snapshots of a table mapping CacheServer
     * ids to HashRingAngle instances that belong to them.
     * @return      A table which maps timestamps to snapshots of CacheServer to HashRingAngle mappings at those times.
     */
    public SortedMap<Integer, Map<Integer, List<HashRingAngle>>> getHashRingAngleHistory() {
        return cacheRedis.getHashRingAngleHistory();
    }

    /**
     * Method from Runnable interface.
     * Starts sub-threads for CacheRedistributor object and ClientRequestHandlerServer object and awaits termination.
     */
    @Override
    public void run() {

        /* Startup cache redistributor and client request handler */
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

        /* Shutdown sub-threads */
        cacheRedisThread.interrupt();
        clientReqHandlerThread.interrupt();
    }

    /**
     * Getter method for the port that ClientRequestHandlerServer is running on.
     * @return      The port that ClientRequestHandlerServer is running on.
     */
    public int getPort() {
        return clientReqHandlerRunnable.getPort();
    }
}