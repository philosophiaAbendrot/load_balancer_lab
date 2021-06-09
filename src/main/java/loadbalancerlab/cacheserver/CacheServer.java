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

public class CacheServer implements Runnable {
    /**
     * Associated RequestMonitor which keeps track of load on a CacheServer instance
     */
    RequestMonitor reqMonitor;
    /**
     * Runnable implementation which wraps around 'reqMonitor' variable
     */
    RequestMonitorRunnable reqMonitorRunnable;
    /**
     * Thread running the runnable implementation 'reqMonitorRunnable'
     */
    Thread reqMonitorThread;

    /**
     * Port number on which the cache server instance is running
     */
    private volatile int port;

    /**
     * Used for logging
     */
    private Logger logger;

    /**
     * Array of available ports on which CacheServers can run.
     * These are allocated to be all ports between 37_000 and 37_099
     */
    int[] selectablePorts = new int[100];

    public CacheServer(RequestMonitor _reqMonitor) {
        reqMonitor = _reqMonitor;
        reqMonitorRunnable = new RequestMonitorRunnable(reqMonitor);
        reqMonitorThread = new Thread(reqMonitorRunnable);

        Random rand = new Random();
        initializeSelectablePorts();
        logger = new Logger("CacheServer");
    }

    /**
     * Helper method which initializes 'selectablePorts' field to be an array of all ports between 37_000 and 37_099
     */
    private void initializeSelectablePorts() {
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;
    }

    /**
     * @return getter method which returns the port that the CacheServer instance is running on
     */
    public int getPort() {
        return port;
    }

    /**
     * Setter method for setting the port on which the CacheServer instance is running
     * @param _port: the port number
     */
    public void setPort(int _port) {
        port = _port;
    }
    /**
     * Method inherited from Runnable interface
     * Starts server and request monitor thread
     */
    @Override
    public void run() {
        logger.log("Started CacheServer thread", Logger.LogType.THREAD_MANAGEMENT);
        // start server
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        HttpHandler clientReqHandler = new CacheServerClientRequestHandler(reqMonitor);
        HttpHandler capacityFactorRequestHandler = new CapacityFactorRequestHandler(reqMonitor);
        HttpServer server = null;

        for (int i = 0; i < selectablePorts.length; i++) {
            port = selectablePorts[i];
            logger.log("attempting to start server on port " + port, Logger.LogType.CACHE_SERVER_STARTUP);

            try {
                InetAddress host = InetAddress.getByName("127.0.0.1");
                InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                server = HttpServer.create(socketAddress, 0);
                server.createContext("/capacity-factor", capacityFactorRequestHandler);
                server.createContext("/", clientReqHandler);
                server.setExecutor(threadPoolExecutor);
                logger.log("Server started on " + socketAddress.toString(), Logger.LogType.CACHE_SERVER_STARTUP);
                break;
            } catch(IOException e) {
                logger.log("Failed to start server on port " + port, Logger.LogType.CACHE_SERVER_STARTUP);
            }
        }

        // start request monitor thread
        reqMonitorThread.start();

        // start server
        server.start();
        logger.log("Server started on port " + port, Logger.LogType.CACHE_SERVER_STARTUP);

        while(true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log("CacheServer thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
                // shutdown associated telemetry curator thread
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
