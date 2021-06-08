package loadbalancerlab;

import loadbalancerlab.cacheserver.CacheServerClientRequestHandler;
import loadbalancerlab.cacheserver.RequestMonitor;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.CacheServerManagerRunnable;
import loadbalancerlab.client.ClientManagerRunnable;
import loadbalancerlab.factory.ClientFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.loadbalancer.*;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;
import loadbalancerlab.vendor.Graph;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.client.Client;
import loadbalancerlab.cacheservermanager.CacheServerManager;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Executor {
    final static int NUM_CLIENTS = 50;
    long maxDemandTime;
    final static int CLIENT_REQUEST_SEND_TIME = 40_000;
    final static int STARTUP_SERVER_COUNT = 10;
    final static int CONSTANT_DEMAND_REST_INTERVAL = 1_000;
    Random rand;

    public Executor() {
        maxDemandTime = System.currentTimeMillis() + 20_000;
    }

    /**
     * Starts, runs and shuts down entire simulation system
     */
    public void start() {
        this.rand = new Random();
        Logger.configure(new Logger.LogType[] { Logger.LogType.CAPACITY_MODULATION });
        Logger.log("Run | started Run thread", Logger.LogType.ALWAYS_PRINT);

        // configure classes
        Config config = new Config();
        configure(config);

        // instantiate factories and other shared services
        CacheServerFactory cacheServerFactory = new CacheServerFactory();
        HttpClientFactory httpClientFactory = new HttpClientFactory();
        RequestDecoder reqDecoder = new RequestDecoder();
        ClientFactory clientFactory = new ClientFactory();

        // start cache server manager thread
        CacheServerManager cacheServerManager = new CacheServerManager(cacheServerFactory, httpClientFactory, reqDecoder);
        CacheServerManagerRunnable cacheServerManagerRunnable = new CacheServerManagerRunnable(cacheServerFactory, httpClientFactory, reqDecoder, cacheServerManager);
        Thread cacheServerManagerThread = new Thread(cacheServerManagerRunnable);
        cacheServerManagerThread.start();

        int cacheServerManagerPort;

        // wait for cache server manager to start up and record the port it's running on
        while ((cacheServerManagerPort = cacheServerManagerRunnable.getPort()) == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.log("Executor | CacheServerManager startup loop interrupted", Logger.LogType.STARTUP_SEQUENCE);
            }
        }

        Logger.log("Executor | CacheServerManager running on port " + cacheServerManagerPort, Logger.LogType.STARTUP_SEQUENCE);

        // instantiate and start load balancer
        LoadBalancerRunnable loadBalancer = new LoadBalancerRunnable(cacheServerManagerPort);
        Thread loadBalancerThread = new Thread(loadBalancer);
        loadBalancerThread.start();
        int loadBalancerPort;

        // wait for load balancer to start and port to be set
        while ((loadBalancerPort = loadBalancer.getPort()) == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.log("Executor | LoadBalancer startup waiting loop interrupted", Logger.LogType.STARTUP_SEQUENCE);
            }
        }

        Logger.log("Executor | LoadBalancer running on port " + loadBalancerPort, Logger.LogType.STARTUP_SEQUENCE);

        // set load balancer port on Client class
        Client.setLoadBalancerPort(loadBalancerPort);

        long requestStartTime = System.currentTimeMillis() + 1_000;

        // startup ClientManager class
        ClientManagerRunnable clientManagerRunnable = new ClientManagerRunnable(clientFactory, maxDemandTime, requestStartTime, httpClientFactory);
        Thread clientManagerRunnableThread = new Thread(clientManagerRunnable);
        clientManagerRunnableThread.start();

        // let simulation run
        try {
            Thread.sleep(CLIENT_REQUEST_SEND_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Logger.log("Executor | Simulation interrupted", Logger.LogType.THREAD_MANAGEMENT);
        }

        // interrupt ClientManager class
        Logger.log("Run | shutdown stage 1: shutdown client threads", Logger.LogType.THREAD_MANAGEMENT);
        clientManagerRunnableThread.interrupt();

        // allow time to shut down client threads
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // collect data from load balancer
//        Logger.log("collecting request log data from load balancer", Logger.LogType.RECORDING_DATA);
//        SortedMap<Integer, Integer> loadBalancerRequestLog = loadBalancer.deliverData();

        // shutdown load balancer
        Logger.log("Run | shutdown stage 2: Shutdown LoadBalancer thread", Logger.LogType.THREAD_MANAGEMENT);
        loadBalancerThread.interrupt();

        // allow time to shut down load balancer system
        try {
            Thread.sleep(2_000);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        // collect data from CacheServerManager instance about how many cache servers were active at each second
        SortedMap<Integer, Integer> serverCountLog = cacheServerManager.deliverData();

        // shutdown CacheServerManager instance
        cacheServerManagerThread.interrupt();
        Logger.log("Run | shutdown stage 3: Shutdown CacheServerManager thread", Logger.LogType.THREAD_MANAGEMENT);
        Logger.log("Run | terminated Run thread", Logger.LogType.THREAD_MANAGEMENT);

        // Graph collected metrics
        List<Double> synthesizedClientRequestLogOutput = new ArrayList<>();
        List<Double> loadBalancerRequestLogOutput = new ArrayList<>();
        List<Double> serverCountLogOutput = new ArrayList<>();

//        for (Integer value : synthesizedClientRequestLog.values())
//            synthesizedClientRequestLogOutput.add((double)value);

//        for (Integer value : loadBalancerRequestLog.values())
//            loadBalancerRequestLogOutput.add((double)value);

        for (Map.Entry<Integer, Integer> entry : serverCountLog.entrySet())
            serverCountLogOutput.add((double) entry.getValue());


        // graph client request requests sent vs time
//        Graph mainPanel = new Graph(synthesizedClientRequestLogOutput);
//        mainPanel.setPreferredSize(new Dimension(800, 600));
//        JFrame frame = new JFrame("Client request output");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.getContentPane().add(mainPanel);
//        frame.pack();
//        frame.setLocationRelativeTo(null);
//        frame.setVisible(true);

        // graph load balancer requests received vs time
//        Graph secondPanel = new Graph(loadBalancerRequestLogOutput);
//        secondPanel.setPreferredSize(new Dimension(800, 600));
//        JFrame secondFrame = new JFrame("Load Balancer requests received");
//        secondFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        secondFrame.getContentPane().add(secondPanel);
//        secondFrame.pack();
//        secondFrame.setLocationRelativeTo(null);
//        secondFrame.setVisible(true);

        // graph cacheServerManager cache server count vs time
        Graph thirdPanel = new Graph(serverCountLogOutput);
        thirdPanel.setPreferredSize((new Dimension(800, 600)));
        JFrame thirdFrame = new JFrame("Cache servers active vs time");
        thirdFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        thirdFrame.getContentPane().add(thirdPanel);
        thirdFrame.pack();
        thirdFrame.setLocationRelativeTo(null);
        thirdFrame.setVisible(true);
    }

    public static void main(String[] args) {
        new Executor().start();
    }

    private void configure(Config config) {
        // CONFIGURATION
        // configure CacheServer package
        RequestMonitor.configure(config);
        // configure CacheServerManager package
        CacheInfoServerRunnable.configure(config); // being called
        CacheServerManager.configure(config); // being called
        // configure LoadBalancer package
        CacheRedistributor.configure(config); // being called
        CacheRedistributorRunnable.configure(config); // being called

        CacheServerClientRequestHandler.configure(config);
        LoadBalancerClientRequestHandler.configure(config);
        ClientRequestHandlerServer.configure(config);
        HashRing.configure(config);
        // configure Client package
        ClientManagerRunnable.configure(config);
    }
}
