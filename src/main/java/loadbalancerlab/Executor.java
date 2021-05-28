package loadbalancerlab;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import loadbalancerlab.loadbalancer.LoadBalancer;
import loadbalancerlab.factory.CapacityFactorMonitorFactoryImpl;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.services.ConstantDemandFunctionImpl;
import loadbalancerlab.vendor.Graph;
import loadbalancerlab.util.Logger;
import loadbalancerlab.factory.CacheServerFactoryImpl;
import loadbalancerlab.client.Client;

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

    // start simulation
    public void start() {
        this.rand = new Random();
        Logger.configure(new Logger.LogType[] { Logger.LogType.CAPACITY_MODULATION });
        Logger.log("Run | started Run thread", Logger.LogType.THREAD_MANAGEMENT);

        // start cache server manager thread
        CacheServerManager cacheServerManager = new CacheServerManager(new CacheServerFactoryImpl());
        Thread cacheServerManagerThread = new Thread(cacheServerManager);
        cacheServerManagerThread.start();

        int cacheServerManagerPort;

        while ((cacheServerManagerPort = cacheServerManager.getPort()) == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("Run | CacheServerManager startup waiting loop interrupted");
            }
        }

        System.out.println("CacheServerManager running on port " + cacheServerManagerPort);

        // start load balancer thread
        LoadBalancer loadBalancer = new LoadBalancer(STARTUP_SERVER_COUNT, cacheServerManagerPort, new CapacityFactorMonitorFactoryImpl(), new HttpClientFactoryImpl());
        Thread loadBalancerThread = new Thread(loadBalancer);
        loadBalancerThread.start();
        int loadBalancerPort;

        // wait for load balancer to start and port to be set
        while ((loadBalancerPort = loadBalancer.getPort()) == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("Run | LoadBalancer startup waiting loop interrupted");
            }
        }

        System.out.println("LoadBalancer running on port " + loadBalancerPort);

        // set load balancer port on Client class
        Client.setLoadBalancerPort(loadBalancerPort);

        List<Thread> clientThreads = new ArrayList<>();
        List<Client> clients = new ArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            Client client = new Client(Integer.toString(i), this.maxDemandTime, new ConstantDemandFunctionImpl(CONSTANT_DEMAND_REST_INTERVAL), new HttpClientFactoryImpl(), System.currentTimeMillis() + (long)((new Random()).nextInt(15000)), this.rand.nextInt(10_000));
            Thread clientThread = new Thread(client);
            clients.add(client);
            clientThreads.add(clientThread);
        }

        for (Thread clientThread : clientThreads)
            clientThread.start();

        // send requests from clients
        try {
            Thread.sleep(CLIENT_REQUEST_SEND_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // shutdown client threads and synthesize data from the client servers
        SortedMap<Integer, Integer> synthesizedClientRequestLog = new TreeMap<>();
        List<Integer> synthesizedRequestList = new ArrayList<>();

        // collate timestamp lists from all the clients into one list
        for (Client client : clients)
            synthesizedRequestList.addAll(client.deliverData());

        // interrupt client threads to initiate their shutdown
        for (Thread clientThread : clientThreads)
            clientThread.interrupt();

        // convert the synthesized list into a sorted map mapping timestamps to the number of requests sent in the duration
        // of that timestamp
        for (Integer timestamp : synthesizedRequestList) {
            if (synthesizedClientRequestLog.containsKey(timestamp)) {
                Integer prev = synthesizedClientRequestLog.get(timestamp);
                synthesizedClientRequestLog.put(timestamp, prev + 1);
            } else {
                synthesizedClientRequestLog.put(timestamp, 1);
            }
        }

        Logger.log("Run | shutdown stage 1: shutdown client threads", Logger.LogType.THREAD_MANAGEMENT);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // collect data from load balancer
        Logger.log("collecting request log data from load balancer", Logger.LogType.RECORDING_DATA);
        SortedMap<Integer, Integer> loadBalancerRequestLog = loadBalancer.deliverData();

        // shutdown load balancer
        loadBalancerThread.interrupt();

        Logger.log("Run | shutdown stage 2: Shutdown LoadBalancer thread", Logger.LogType.THREAD_MANAGEMENT);

        try {
            Thread.sleep(5_000);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        // collect data from CacheServerManager instance
        SortedMap<Integer, Integer> serverCountLog = cacheServerManager.deliverData();

        // shutdown CacheServerManager instance
        cacheServerManagerThread.interrupt();
        Logger.log("Run | shutdown stage 3: Shutdown CacheServerManager thread", Logger.LogType.THREAD_MANAGEMENT);
        Logger.log("Run | terminated Run thread", Logger.LogType.THREAD_MANAGEMENT);

        // Graph collected metrics
        List<Double> synthesizedClientRequestLogOutput = new ArrayList<>();
        List<Double> loadBalancerRequestLogOutput = new ArrayList<>();
        List<Double> serverCountLogOutput = new ArrayList<>();

        for (Integer value : synthesizedClientRequestLog.values())
            synthesizedClientRequestLogOutput.add((double)value);

        for (Integer value : loadBalancerRequestLog.values())
            loadBalancerRequestLogOutput.add((double)value);

        for (Map.Entry<Integer, Integer> entry : serverCountLog.entrySet())
            serverCountLogOutput.add((double) entry.getValue());


        // graph client request requests sent vs time
        Graph mainPanel = new Graph(synthesizedClientRequestLogOutput);
        mainPanel.setPreferredSize(new Dimension(800, 600));
        JFrame frame = new JFrame("Client request output");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // graph load balancer requests received vs time
        Graph secondPanel = new Graph(loadBalancerRequestLogOutput);
        secondPanel.setPreferredSize(new Dimension(800, 600));
        JFrame secondFrame = new JFrame("Load Balancer requests received");
        secondFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        secondFrame.getContentPane().add(secondPanel);
        secondFrame.pack();
        secondFrame.setLocationRelativeTo(null);
        secondFrame.setVisible(true);

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
}
