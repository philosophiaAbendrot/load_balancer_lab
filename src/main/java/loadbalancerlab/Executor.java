package loadbalancerlab;

import loadbalancerlab.cacheserver.CacheServerClientRequestHandler;
import loadbalancerlab.cacheserver.RequestMonitor;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.CacheServerManagerRunnable;
import loadbalancerlab.client.ClientManagerRunnable;
import loadbalancerlab.factory.ClientFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.loadbalancer.*;
import loadbalancerlab.shared.AngleDataProcessor;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;
import loadbalancerlab.vendor.Graph;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.client.Client;
import loadbalancerlab.cacheservermanager.CacheServerManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Executor {
    long maxDemandTime;
    static int simulationTime;
    Random rand;
    Logger logger;
    CacheServerFactory cacheServerFactory;
    HttpClientFactory httpClientFactory;
    RequestDecoder reqDecoder;
    ClientFactory clientFactory;
    CacheServerManager cacheServerManager;
    CacheServerManagerRunnable cacheServerManagerRunnable;
    Thread cacheServerManagerThread;
    LoadBalancerRunnable loadBalancer;
    Thread loadBalancerThread;
    ClientManagerRunnable clientManagerRunnable;
    Thread clientManagerThread;

    // execution data
    List<Double> synthesizedClientRequestLogOutput;
    List<Double> loadBalancerRequestLogOutput;
    List<Double> serverCountLogOutput;
    String[][] cacheServerCfData;
    SortedMap<Integer, Integer> serverCountLog;
    String[][] numAnglesByServerByTime;

    public static void configure( Config config ) {
        simulationTime = config.getSimulationTime();
    }

    public Executor() {
        maxDemandTime = System.currentTimeMillis() + 20_000;
    }

    /**
     * Starts, runs and shuts down entire simulation system
     */
    public void start( Config config ) {
        this.rand = new Random();
//        Logger.setPrintAll(true);
        logger = new Logger("Executor");
        Logger.configure(new Logger.LogType[] {  });
        logger.log("started Run thread", Logger.LogType.STARTUP_SEQUENCE);

        // configure classes
        configureComponents(config);

        // instantiate factories and other shared services
        instantiateFactories();

        // startup threads
        startupThreads();

        // let simulation run
        try {
            Thread.sleep(simulationTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.log("Simulation interrupted", Logger.LogType.THREAD_MANAGEMENT);
        }

        // interrupt threads
        shutdownThreads();

        // collect simulation data
        collectData(config.getRingSize());

        // print data to csv
        printData();
    }

    public static void main(String[] args) {
        Config config = new Config();
        Executor.configure(config);
        new Executor().start(config);
    }

    private void configureComponents(Config config) {
        // CONFIGURATION
        // configure CacheServer package
        RequestMonitor.configure(config);
        // configure CacheServerManager package
        CacheInfoServerRunnable.configure(config); // being called
        CacheServerManager.configure(config); // being called
        CacheServerManagerRunnable.configure(config);
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

    private void instantiateFactories() {
        cacheServerFactory = new CacheServerFactory();
        httpClientFactory = new HttpClientFactory();
        reqDecoder = new RequestDecoder();
        clientFactory = new ClientFactory();
    }

    /**
     * Compiles data from simulation
     */
    private void collectData(int hashRingSize) {
        // collect data from CacheServerManager instance about how many cache servers were active at each second
        serverCountLog = cacheServerManager.deliverServerCountData();

        // Graph collected metrics
        synthesizedClientRequestLogOutput = new ArrayList<>();
        loadBalancerRequestLogOutput = new ArrayList<>();

        int earliestTime = serverCountLog.firstKey();
        int latestTime = serverCountLog.lastKey();

        serverCountLogOutput = new ArrayList<>();

        // initialize serverCountLogOutput to have one entry for each second
        for (int timestamp = earliestTime; timestamp <= latestTime; timestamp++)
            serverCountLogOutput.add(0.0d);

//        for (Integer value : synthesizedClientRequestLog.values())
//            synthesizedClientRequestLogOutput.add((double)value);

//        for (Integer value : loadBalancerRequestLog.values())
//            loadBalancerRequestLogOutput.add((double)value);

        // add collected data
        for (Map.Entry<Integer, Integer> entry : serverCountLog.entrySet()) {
            int timestamp = entry.getKey();
            serverCountLogOutput.set(timestamp - earliestTime, (double) entry.getValue());
        }

        // collect HashRingAngle data
        SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory = loadBalancer.getHashRingAngleHistory();
        AngleDataProcessor angleDataProcessor = new AngleDataProcessor(angleHistory, hashRingSize);
        numAnglesByServerByTime = angleDataProcessor.getNumAnglesByTime();

        cacheServerCfData = cacheServerManager.deliverCfData();
    }

    private void startupThreads() {
        // start cache server manager thread
        cacheServerManager = new CacheServerManager(cacheServerFactory, httpClientFactory, reqDecoder);
        cacheServerManagerRunnable = new CacheServerManagerRunnable(cacheServerFactory, httpClientFactory, reqDecoder, cacheServerManager);
        cacheServerManagerThread = new Thread(cacheServerManagerRunnable);

        cacheServerManagerThread.start();

        int cacheServerManagerPort;

        // wait for cache server manager to start up and record the port it's running on
        while ((cacheServerManagerPort = cacheServerManagerRunnable.getPort()) == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.log("CacheServerManager startup loop interrupted", Logger.LogType.STARTUP_SEQUENCE);
            }
        }

        logger.log("CacheServerManager running on port " + cacheServerManagerPort, Logger.LogType.STARTUP_SEQUENCE);

        // instantiate and start load balancer
        loadBalancer = new LoadBalancerRunnable(cacheServerManagerPort);
        loadBalancerThread = new Thread(loadBalancer);
        loadBalancerThread.start();
        int loadBalancerPort;

        // wait for load balancer to start and port to be set
        while ((loadBalancerPort = loadBalancer.getPort()) == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.log("LoadBalancer startup waiting loop interrupted", Logger.LogType.STARTUP_SEQUENCE);
            }
        }

        logger.log("LoadBalancer running on port " + loadBalancerPort, Logger.LogType.STARTUP_SEQUENCE);

        // set load balancer port on Client class
        Client.setLoadBalancerPort(loadBalancerPort);

        long requestStartTime = System.currentTimeMillis() + 1_000;

        // startup ClientManager class
        clientManagerRunnable = new ClientManagerRunnable(clientFactory, maxDemandTime, requestStartTime, httpClientFactory, reqDecoder);
        clientManagerThread = new Thread(clientManagerRunnable);
        clientManagerThread.start();
    }

    /**
     * Prints simulation data to csv
     */
    private void printData() {
        System.out.println("printing num servers vs time");

        try {
            // write data on server count vs time
            FileWriter out = new FileWriter("csv_output/num_servers_vs_time.csv");
            String[] headers = { "time", "cache servers active" };
            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(headers))) {
                System.out.println("printer started 1");
                for (Map.Entry<Integer, Integer> entry : serverCountLog.entrySet()) {
                    System.out.println("key = " + entry.getKey() + " | value = " + entry.getValue());
                    printer.printRecord(entry.getKey(), entry.getValue());
                }
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("printing cf vs time");

        try {
            // write data on cf vs time
            FileWriter out = new FileWriter("csv_output/cf_vs_time.csv");
            String[] headers = cacheServerCfData[0];
            String[][] content = new String[cacheServerCfData.length - 1][cacheServerCfData[0].length];

            for (int i = 1; i < cacheServerCfData.length; i++) {
                content[i - 1] = cacheServerCfData[i];
            }

            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(headers))) {
                for (int i = 0; i < content.length; i++) {
                    System.out.println("content = " + Arrays.toString(content[i]));
                    printer.printRecord(content[i]);
                }
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Printing num angles vs time by server");

        try {
            // write data on number of angles by server by time
            FileWriter out = new FileWriter("csv_output/num_angles_vs_time.csv");
            String[] headers = numAnglesByServerByTime[0];
            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(headers))) {
                for (int i = 1; i < numAnglesByServerByTime.length; i++) {
                    printer.printRecord(numAnglesByServerByTime[i]);
                }
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Graphs simulation data
     */
    private void graphData() {
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


        Graph thirdPanel = new Graph(serverCountLogOutput);
        thirdPanel.setPreferredSize((new Dimension(800, 600)));
        JFrame thirdFrame = new JFrame("Cache servers active vs time");
        thirdFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        thirdFrame.getContentPane().add(thirdPanel);
        thirdFrame.pack();
        thirdFrame.setLocationRelativeTo(null);
        thirdFrame.setVisible(true);
    }

    private void shutdownThreads() {
        // interrupt ClientManager class
        logger.log("shutdown stage 1: shutdown client threads", Logger.LogType.THREAD_MANAGEMENT);
        clientManagerThread.interrupt();


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
        logger.log("shutdown stage 2: Shutdown LoadBalancer thread", Logger.LogType.THREAD_MANAGEMENT);
        loadBalancerThread.interrupt();

        // allow time to shut down load balancer system
        try {
            Thread.sleep(2_000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        // shutdown CacheServerManager instance
        cacheServerManagerThread.interrupt();
        logger.log("shutdown stage 3: Shutdown CacheServerManager thread", Logger.LogType.THREAD_MANAGEMENT);
        logger.log("terminated Run thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}
