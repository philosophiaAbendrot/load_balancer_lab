package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.CapacityFactorMonitorFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Logger;

import org.apache.http.*;
import org.apache.http.protocol.*;

import java.util.*;

public class LoadBalancer implements Runnable {
    private final int HASH_RING_DENOMINATIONS = 6_000;
    private final int DEFAULT_PORT = 3_000;

    private int port;
    private int cacheServerManagerPort;
    private List<HttpRequestInterceptor> requestInterceptors = new ArrayList<>();
    private List<HttpResponseInterceptor> responseInterceptors = new ArrayList<>();
    private HttpProcessor httpProcessor;
    // maps the port that cache server server is operating on to its capacity factor
    private CapacityFactorMonitorFactory capacityFactorMonitorFactory;
    private Thread capacityFactorMonitorThread = null;
    private int startupServerCount;
    long initiationTime;
    private List<Integer> incomingRequestTimestamps;
    private ClientRequestHandlerServer clientRequestHandlerServer;
    private HttpClientFactory clientFactory;
    private CacheRedistributorImpl cacheRedisImpl;
    private HashRing hashRing;
    private ClientRequestHandler clientRequestHandler;

    public LoadBalancer( int _startupServerCount, int _cacheServerManagerPort, CapacityFactorMonitorFactory capFactMonitorFact, HttpClientFactory _clientFactory ) {
        // dummy port to indicate that the port has not been set
        cacheServerManagerPort = _cacheServerManagerPort;
        port = -1;
        incomingRequestTimestamps = Collections.synchronizedList(new LinkedList<>());
        initiationTime = System.currentTimeMillis();
        startupServerCount = _startupServerCount;
        hashRing = new HashRingImpl();
        cacheRedisImpl = new CacheRedistributorImpl(cacheServerManagerPort, hashRing);
        clientRequestHandler = new ClientRequestHandler(cacheRedisImpl);
        clientRequestHandlerServer = new ClientRequestHandlerServer(clientRequestHandler);
        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
        capacityFactorMonitorFactory = capFactMonitorFact;
        clientFactory = _clientFactory;
    }

    @Override
    public void run() {

    }

    public int getPort() {
        return this.port;
    }

    public SortedMap<Integer, Integer> deliverData() {
        SortedMap<Integer, Integer> output = new TreeMap<>();
        for (Integer timestamp : this.incomingRequestTimestamps) {
            if (output.containsKey(timestamp)) {
                int current = output.get(timestamp);
                output.put(timestamp, current + 1);
            } else {
                output.put(timestamp, 1);
            }
        }

        return output;
    }
}
