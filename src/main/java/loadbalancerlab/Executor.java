package loadbalancerlab;

import loadbalancerlab.cacheserver.CacheServerClientRequestHandler;
import loadbalancerlab.cacheserver.RequestMonitor;
import loadbalancerlab.cacheservermanager.CacheInfoServerRunnable;
import loadbalancerlab.cacheservermanager.CacheServerManagerRunnable;
import loadbalancerlab.client.ClientManagerRunnable;
import loadbalancerlab.factory.*;
import loadbalancerlab.loadbalancer.*;
import loadbalancerlab.shared.AngleDataProcessor;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.RequestDecoder;
import loadbalancerlab.vendor.Graph;
import loadbalancerlab.shared.Logger;
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

/**
 * Main execution class used for setting up, running, and shutting down simulation.
 */
public class Executor {

    /**
     * Timestamp at which demand peaks. Used for certain demand functions. Milliseconds since 1-Jan-1970.
     */
    long maxDemandTime;

    /**
     * Controls how long simulation runs (in milliseconds).
     */
    static int simulationTime;

    /**
     * Logger object used for logging messages to terminal.
     */
    Logger logger;

    /**
     * A factory class which produces CacheServer instances.
     */
    CacheServerFactory cacheServerFactory;

    /**
     * Factory class used for generating CloseableHttpClient instances.
     */
    HttpClientFactory httpClientFactory;

    /**
     * Factory class for producing ServerMonitor instances.
     */
    ServerMonitorFactory serverMonitorFactory;

    /**
     * Used for extracting JSON object from a CloseableHttpResponse object.
     */
    RequestDecoder reqDecoder;

    /**
     * A factory class for producing Client instances.
     */
    ClientFactory clientFactory;

    /**
     * Factory class used to create ServerMonitor objects and CacheInfoServerRunnable
     * objects.
     */
    CacheInfoServerFactory cacheInfoServerFactory;

    /**
     * A server which manages the lifecycle of CacheServer instances.
     * Modulates the number of CacheServers to match the request load.
     */
    CacheServerManager cacheServerManager;

    /**
     * Runnable implementation which serves as a wrapper for CacheServerManager class.
     */
    CacheServerManagerRunnable cacheServerManagerRunnable;

    /**
     * Thread which CacheServerManager object.
     */
    Thread cacheServerManagerThread;

    /**
     * LoadBalancer object which handles delegation and forwarding of incoming client Http requests to CacheServer objects.
     */
    LoadBalancerRunnable loadBalancer;

    /**
     * Thread which runs LoadBalancerRunnable object.
     */
    Thread loadBalancerThread;

    /**
     * Used to manage lifecycle of Client objects.
     */
    ClientManagerRunnable clientManagerRunnable;

    /**
     * Thread instance which runs ClientManagerRunnable object.
     */
    Thread clientManagerThread;

    /* Fields which hold execution data */
    /**
     * A list of doubles which holds number of active CacheServer.
     * Meant to be used by vendor graphing class.
     * The list has one entry for every second of the simulation.
     */
    List<Double> serverCountLogOutput;

    /**
     * A 2d String array representation of a CSV file for CacheServer capacity-factor vs time.
     *
     * The leftmost column lists all timestamps in ascending order from top to bottom.
     * The top row lists all ids of CacheServer instances in ascending order from left to right.
     * The rest of the entries contain capacity factor values.
     */
    String[][] cacheServerCfData;

    /**
     * A sorted map which holds logs about the number of CacheServers active by time.
     * The table maps timestamps (seconds since 1-Jan-1970) to the number of CacheServer objects active.
     */
    SortedMap<Integer, Integer> serverCountLog;

    /**
     * A 2d String array representation of a CSV file for the number of HashRingAngle objects owned by each CacheServer
     * vs time.
     *
     * The top row lists ids of all CacheServers in ascending order from left to right.
     * The leftmost column lists all timestamps in ascending order from top to bottom.
     * The rest of the entries hold the number of HashRingAngle objects owned by a particular CacheServer at a point in
     * time.
     */
    String[][] numAnglesByServerByTime;

    /**
     * A 2d String array representation of a CSV file for the total sweep angle captured by all HashRingAngle objects
     * owned by each CacheServer vs time.
     *
     * The Top row holds ids of all CacheServers in ascending order from left to right.
     * The leftmost column lists all timestamps in ascending order from top to bottom.
     * The rest of the entries hold the total sweep angle captured by all HashRingAngle objects owned by a particular
     * CacheServer at a point in time.
     */
    String[][] sweepAngleByTime;

    /**
     * Static method used to configure static variables in this class.
     * @param config        Configuration object used for configuring various classes.
     */
    public static void configure( Config config ) {
        simulationTime = config.getSimulationTime();
    }

    public Executor() {
        maxDemandTime = System.currentTimeMillis() + 20_000;
    }

    /**
     * Starts, runs and shuts down entire simulation system.
     */
    public void start( Config config ) {

        /* set up logger */
        logger = new Logger("Executor");
        Logger.configure(new Logger.LogType[] {  });
        logger.log("started Run thread", Logger.LogType.THREAD_MANAGEMENT);

        /* Configure classes */
        configureComponents(config);

        /* Instantiate factories and other shared services */
        instantiateFactories();

        /* Startup threads */
        startupThreads();

        /* Let simulation run */
        try {
            Thread.sleep(simulationTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.log("Simulation interrupted", Logger.LogType.THREAD_MANAGEMENT);
        }

        /* Interrupt threads */
        shutdownThreads();

        /* Collect and process simulation data */
        collectData(config.getRingSize());

        /* Print data to csv */
        printData();
    }

    public static void main(String[] args) {
        Config config = new Config();
        Executor.configure(config);
        new Executor().start(config);
    }

    /**
     * Helper method used to configure classes with the given Config object.
     * @param config    Config object used to configure various classes.
     */
    private void configureComponents(Config config) {

        /* Configure CacheServer package */
        RequestMonitor.configure(config);

        /* Configure CacheServerManager package */
        CacheInfoServerRunnable.configure(config);
        CacheServerManager.configure(config);
        CacheServerManagerRunnable.configure(config);

        /* Configure LoadBalancer package */
        CacheRedistributor.configure(config);
        CacheRedistributorRunnable.configure(config);
        CacheServerClientRequestHandler.configure(config);
        LoadBalancerClientRequestHandler.configure(config);
        ClientRequestHandlerServer.configure(config);
        HashRing.configure(config);

        /* Configure Client package */
        ClientManagerRunnable.configure(config);
    }

    /**
     * Helper method used to instantiate factories.
     */
    private void instantiateFactories() {
        cacheServerFactory = new CacheServerFactory();
        httpClientFactory = new HttpClientFactory();
        reqDecoder = new RequestDecoder();
        clientFactory = new ClientFactory();
        cacheInfoServerFactory = new CacheInfoServerFactory();
        serverMonitorFactory = new ServerMonitorFactory();
    }

    /**
     * Helper method used to compiles data from simulation.
     */
    private void collectData(int hashRingSize) {

        /* Collect data from CacheServerManager instance about how many cache servers were active at each second */
        serverCountLog = cacheServerManager.deliverServerCountData();

        /* Graph collected metrics */
        int earliestTime = serverCountLog.firstKey();
        int latestTime = serverCountLog.lastKey();
        serverCountLogOutput = new ArrayList<>();

        /* Initialize serverCountLogOutput to have one entry for each second */
        for (int timestamp = earliestTime; timestamp <= latestTime; timestamp++)
            serverCountLogOutput.add(0.0d);

        /* Add collected data */
        for (Map.Entry<Integer, Integer> entry : serverCountLog.entrySet()) {
            int timestamp = entry.getKey();
            serverCountLogOutput.set(timestamp - earliestTime, (double) entry.getValue());
        }

        /* Collect HashRingAngle data */
        SortedMap<Integer, Map<Integer, List<HashRingAngle>>> angleHistory = loadBalancer.getHashRingAngleHistory();
        AngleDataProcessor angleDataProcessor = new AngleDataProcessor(angleHistory, hashRingSize);
        numAnglesByServerByTime = angleDataProcessor.getNumAnglesByTime();
        sweepAngleByTime = angleDataProcessor.getSweepAngleByTime();

        cacheServerCfData = cacheServerManager.deliverCfData();
    }

    /**
     * Helper method used to startup all threads.
     */
    private void startupThreads() {
        /* Instantiate CacheServerManager and wrap it into a Runnable object and then a Thread object */
        cacheServerManager = new CacheServerManager(cacheServerFactory, httpClientFactory, reqDecoder,
                                                    cacheInfoServerFactory, serverMonitorFactory);
        cacheServerManagerRunnable = new CacheServerManagerRunnable(cacheServerFactory, httpClientFactory, reqDecoder,
                                                                    cacheServerManager, cacheInfoServerFactory,
                                                                    serverMonitorFactory);
        cacheServerManagerThread = new Thread(cacheServerManagerRunnable);

        /* Start cache server manager thread */
        cacheServerManagerThread.start();
        int cacheServerManagerPort;

        /* Wait for cache server manager to start up and record the port it's running on */
        while ((cacheServerManagerPort = cacheServerManagerRunnable.getPort()) == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.log("CacheServerManager startup loop interrupted", Logger.LogType.THREAD_MANAGEMENT);
            }
        }

        logger.log("CacheServerManager running on port " + cacheServerManagerPort, Logger.LogType.THREAD_MANAGEMENT);

        /* Instantiate LoadBalancerRunnable object, wrap it in a Thread object, and start it. */
        loadBalancer = new LoadBalancerRunnable(cacheServerManagerPort);
        loadBalancerThread = new Thread(loadBalancer);
        loadBalancerThread.start();
        int loadBalancerPort;

        /* Wait for load balancer to start and record the port it's running on */
        while ((loadBalancerPort = loadBalancer.getPort()) == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.log("LoadBalancer startup waiting loop interrupted", Logger.LogType.THREAD_MANAGEMENT);
            }
        }

        logger.log("LoadBalancer running on port " + loadBalancerPort, Logger.LogType.THREAD_MANAGEMENT);

        /* Set load balancer port on Client class */
        Client.setLoadBalancerPort(loadBalancerPort);

        long requestStartTime = System.currentTimeMillis() + 1_000;

        /* Instantiate ClientManagerRunnable object, wrap it in a Thread object, and start it. */
        clientManagerRunnable = new ClientManagerRunnable(clientFactory, maxDemandTime, requestStartTime, httpClientFactory, reqDecoder);
        clientManagerThread = new Thread(clientManagerRunnable);
        clientManagerThread.start();
    }

    /**
     * Helper method for printing simulation data to csv files.
     */
    private void printData() {
        logger.log("Printing num servers vs time", Logger.LogType.PRINT_DATA_TO_CSV);

        try {

            /* Write data on server count vs time */
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

        logger.log("Printing cf vs time to csv", Logger.LogType.PRINT_DATA_TO_CSV);

        try {

            /* Write data on cf vs time */
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

        logger.log("Printing num angles vs time by server to csv", Logger.LogType.PRINT_DATA_TO_CSV);

        try {

            /* Write data on number of angles by server by time */
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

        logger.log("Printing sweep angle vs time by server to csv", Logger.LogType.PRINT_DATA_TO_CSV);

        try {

            /* Write sweep angle by time to csv */
            FileWriter out = new FileWriter("csv_output/sweep_angle_by_time.csv");
            String[] headers = sweepAngleByTime[0];

            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(headers))) {
                for (int i = 1; i < sweepAngleByTime.length; i++) {
                    printer.printRecord(sweepAngleByTime[i]);
                }
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method for graphing simulation data using vendor class 'Graph'.
     */
    private void graphData() {

        /* Graph server count vs time data */
        Graph thirdPanel = new Graph(serverCountLogOutput);
        thirdPanel.setPreferredSize((new Dimension(800, 600)));
        JFrame thirdFrame = new JFrame("Cache servers active vs time");
        thirdFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        thirdFrame.getContentPane().add(thirdPanel);
        thirdFrame.pack();
        thirdFrame.setLocationRelativeTo(null);
        thirdFrame.setVisible(true);
    }

    /**
     * Helper method to shutdown threads.
     */
    private void shutdownThreads() {

        /* Interrupt ClientManager class */
        logger.log("shutdown stage 1: shutdown client threads", Logger.LogType.THREAD_MANAGEMENT);
        clientManagerThread.interrupt();

        /* Allow time to shut down client threads */
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /* Shutdown load balancer */
        logger.log("shutdown stage 2: Shutdown LoadBalancer thread", Logger.LogType.THREAD_MANAGEMENT);
        loadBalancerThread.interrupt();

        /* Allow time to shut down load balancer system */
        try {
            Thread.sleep(2_000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        /* Shutdown CacheServerManager instance */
        cacheServerManagerThread.interrupt();
        logger.log("shutdown stage 3: Shutdown CacheServerManager thread", Logger.LogType.THREAD_MANAGEMENT);
        logger.log("terminated Run thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}
