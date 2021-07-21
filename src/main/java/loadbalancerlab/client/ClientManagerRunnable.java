package loadbalancerlab.client;

import loadbalancerlab.factory.ClientFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages lifecycle of Client objects.
 * Implementation of Runnable interface.
 */
public class ClientManagerRunnable implements Runnable {

    /**
     * Number of clients instances that the ClientManagerRunnable will spawn.
     */
    private static int numClients;

    /**
     * A list that holds all the client threads that have been spawned by this instance.
     */
    List<Thread> clientThreads;

    /**
     * An instance of ClientFactory, which generates Client instances.
     */
    ClientFactory clientFactory;

    /**
     * An instance of HttpClientFactory, which generates HttpClient instances which are used to send Http requests.
     */
    HttpClientFactory httpClientFactory;

    /**
     * The time at which max demand occurs. This field is only relevant for time-varying demand functions.
     * (Milliseconds since 1-Jan-1970)
     */
    long maxDemandTime;

    /**
     * Time time at which requests start sending. (Milliseconds since 1-Jan-1970)
     */
    long requestStartTime;

    /**
     * The basic rest time between requests in milliseconds. This base time is modified by demand functions.
     */
    static int restInterval;

    /**
     * Used for parsing JSON from responses
     */
    RequestDecoder reqDecoder;

    /**
     * Variable which controls time (in milliseconds) between runs of the main loop in run() method.
     */
    static final int TICK_INTERVAL = 100;

    /**
     * Constructor
     * @param clientFactory         Factory object which generates Client instances for sending requests to
     *                              LoadBalancer.
     * @param maxDemandTime         The time at which max request load occurs. (Milliseconds since 1-Jan-1970)
     * @param requestStartTime      The time at which requests start sending. (Milliseconds since 1-Jan-1970)
     * @param httpClientFactory     Factory object which generates CloseableHttpClient objects for sending http
     *                              requests.
     * @param reqDecoder            Object used to extract JSON object from a CloseableHttpResponse object.
     */
    public ClientManagerRunnable( ClientFactory clientFactory, long maxDemandTime, long requestStartTime,
                                  HttpClientFactory httpClientFactory, RequestDecoder reqDecoder ) {
        this.clientFactory = clientFactory;
        this.maxDemandTime = maxDemandTime;
        this.requestStartTime = requestStartTime;
        this.httpClientFactory = httpClientFactory;
        this.reqDecoder = reqDecoder;
    }

    /**
     * Allow configurations of static variables using a Config instance.
     * @param config    Config object used to configure various classes.
     */
    public static void configure( Config config ) {
        numClients = config.getNumClients();
        restInterval = config.getDemandFunctionRestInterval();
    }

    /**
     * Starts up client threads.
     * Implementation of Runnable interface method.
     * When this thread is interrupted, it interrupts all client threads spawned by it.
     */
    @Override
    public void run() {
        clientThreads = new ArrayList<>();

        /* Generate client threads */
        for (int i = 0; i < numClients; i++) {
            Client client = clientFactory.buildClient(maxDemandTime, new ConstantDemandFunctionImpl(restInterval), httpClientFactory, requestStartTime, reqDecoder);
            Thread clientThread = new Thread(client);
            clientThreads.add(clientThread);
        }

        /* Start client threads */
        for (Thread thread : clientThreads)
            thread.start();

        /* Await termination */
        while (true) {
            try {
                Thread.sleep(TICK_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                break;
            }
        }

        /* Interrupt client threads */
        for (Thread thread : clientThreads) {
            thread.interrupt();
        }
    }
}