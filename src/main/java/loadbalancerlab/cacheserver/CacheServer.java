package loadbalancerlab.cacheserver;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import loadbalancerlab.shared.Logger;

/**
 * Handles client requests which are forwarded by LoadBalancerRunnable using ClientRequestHandler object.
 * Monitors its usage levels and handles requests for updates by LoadBalancerRunnable using
 * CapacityFactorRequestsHandler object.
 */
public class CacheServer implements Runnable {

    /**
     * Associated RequestMonitor which keeps track of the capacity factor of this object and data on the incoming
     * requests.
     */
    RequestMonitor reqMonitor;

    /**
     * Runnable implementation which wraps around RequestMonitor object.
     */
    RequestMonitorRunnable reqMonitorRunnable;

    /**
     * Thread running the RequestMonitorRunnable object.
     */
    Thread reqMonitorThread;

    /**
     * Port number on which the CacheServer instance is running.
     */
    private volatile int port;

    /**
     * Object used for logging.
     */
    private Logger logger;

    /**
     * Array of available ports on which CacheServers can run.
     * These are allocated to be all ports between 37_000 and 37_099.
     */
    int[] selectablePorts = new int[100];

    /**
     * Constructor
     * @param reqMonitor        RequestMonitor object used to keep track of the capacity factor of this object.
     */
    public CacheServer(RequestMonitor reqMonitor) {

        /* Initialize request monitor and wrap it in a Runnable object and then assign it to a Thread object */
        this.reqMonitor = reqMonitor;
        reqMonitorRunnable = new RequestMonitorRunnable(this.reqMonitor);
        reqMonitorThread = new Thread(reqMonitorRunnable);
        logger = new Logger("CacheServer");

        /* Initialize array of ports that this object can run on */
        initializeSelectablePorts();
    }

    /**
     * Helper method which initializes 'selectablePorts' field to be an array of all ports between 37_000 and 37_099.
     */
    private void initializeSelectablePorts() {
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;
    }

    /**
     * Getter method for returning the port that the CacheServer instance is running on.
     * @return      The port that the CacheServer instance is running on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Setter method for setting the port on which the CacheServer instance is running.
     * @param port: the port on which this CacheServer object is running.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Method inherited from Runnable interface.
     * Starts server and request monitor thread.
     */
    @Override
    public void run() {
        /* Start server */
        logger.log("Started CacheServer thread", Logger.LogType.THREAD_MANAGEMENT);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

        /* Used for handling client requests forwarded by the load balancer */
        HttpHandler clientReqHandler = new CacheServerClientRequestHandler(reqMonitor);

        /* Used for handling requests for updates on capacity factor */
        HttpHandler capacityFactorRequestHandler = new CapacityFactorRequestHandler(reqMonitor);
        HttpServer server = null;

        /* Attempt to start CacheServer on random ports among the selectable ports */
        Random rand = new Random();

        for (int i = 0; i < selectablePorts.length; i++) {
            int randIdx = rand.nextInt(selectablePorts.length);
            port = selectablePorts[randIdx];
            logger.log("attempting to start server on port " + port, Logger.LogType.CAPACITY_MODULATION);

            /* Retry until the port starts successfully */
            try {
                InetAddress host = InetAddress.getByName("127.0.0.1");
                InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                server = HttpServer.create(socketAddress, 0);
                server.createContext("/capacity-factor", capacityFactorRequestHandler);
                server.createContext("/", clientReqHandler);
                server.setExecutor(threadPoolExecutor);
                logger.log("Server started on " + socketAddress.toString(), Logger.LogType.CAPACITY_MODULATION);
                break;
            } catch(IOException e) {
                logger.log("Failed to start server on port " + port, Logger.LogType.CAPACITY_MODULATION);
            }
        }

        /* Start request monitor thread */
        reqMonitorThread.start();

        /* Start server */
        server.start();
        logger.log("Server started on port " + port, Logger.LogType.CAPACITY_MODULATION);

        while(true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

                /* Start shutdown */
                Thread.currentThread().interrupt();
                logger.log("CacheServer thread interrupted", Logger.LogType.THREAD_MANAGEMENT);

                /* Shutdown associated telemetry curator thread */
                reqMonitorThread.interrupt();
                logger.log("Request Monitor thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
                server.stop(3);
                threadPoolExecutor.shutdown();
                break;
            }
        }

        logger.log("Terminated CacheServer thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}
