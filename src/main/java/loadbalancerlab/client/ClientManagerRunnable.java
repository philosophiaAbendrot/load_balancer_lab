package loadbalancerlab.client;

import loadbalancerlab.factory.ClientFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;

import java.util.List;

public class ClientManagerRunnable implements Runnable {
    /**
     * Number of clients instances that the ClientManagerRunnable will spawn
     */
    private static int numClients;
    /**
     * A list that holds all the client threads that have been spawned by this instance
     */
    List<Thread> clientThreads;
    /**
     * An instance of ClientFactory, which generates Client instances
     */
    ClientFactory clientFactory;
    /**
     * An instance of HttpClientFactory, which generates HttpClient instances which are used to send Http requests
     */
    HttpClientFactory httpClientFactory;
    /**
     * The time at which max demand occurs. This field is only relevant for time-varying demand functions.
     * Milliseconds since 1-Jan-1970
     */
    long maxDemandTime;
    /**
     * Time time at which requests start sending. Milliseconds since 1-Jan-1970.
     */
    long requestStartTime;
    /**
     * The basic rest time between requests in milliseconds. This base time is modified by demand functions.
     */
    int restInterval = 200;
    static final int TICK_INTERVAL = 100;
    String resourceName = "Chooder_Bunny.jpg";

    public ClientManagerRunnable( ClientFactory clientFactory, long maxDemandTime, long requestStartTime, HttpClientFactory httpClientFactory ) {
        this.clientFactory = clientFactory;
        this.maxDemandTime = maxDemandTime;
        this.requestStartTime = requestStartTime;
        this.httpClientFactory = httpClientFactory;
    }

    /** Allow configurations of static variables using a Config instance
     * @param config: Config instance which holds all configurations
     */
    public static void configure( Config config ) {
        numClients = config.getNumClients();
    }

    /** Implementation of Runnable interface method
     * Starts up client threads
     * When this thread is interrupted, it interrupts all client threads spawned by it.
     */
    @Override
    public void run() {
        // generate client threads
        for (int i = 0; i < numClients; i++) {
            clientThreads.add(new Thread(clientFactory.buildClient(maxDemandTime, new ConstantDemandFunctionImpl(restInterval), httpClientFactory, requestStartTime, resourceName)));
        }

        // start threads
        for (Thread thread : clientThreads)
            thread.start();

        while (true) {
            try {
                Thread.sleep(TICK_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // interrupt client threads
        for (Thread thread : clientThreads) {
            thread.interrupt();
        }
    }
}