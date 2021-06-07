package loadbalancerlab.client;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;

import java.util.List;

public class ClientManagerRunnable implements Runnable {
    private static int numClients;
    List<Thread> clientThreads;

    public ClientManagerRunnable( HttpClientFactory clientFactory ) {

    }

    public static void configure( Config config ) {
        numClients = config.getNumClients();
    }

    @Override
    public void run() {

    }
}