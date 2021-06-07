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
    RequestMonitor reqMonitor;
    RequestMonitorRunnable reqMonitorRunnable;
    Thread reqMonitorThread;

    private volatile int port;
    int[] selectablePorts = new int[100];

    public CacheServer(RequestMonitor _reqMonitor) {
        reqMonitor = _reqMonitor;
        reqMonitorRunnable = new RequestMonitorRunnable(reqMonitor);
        reqMonitorThread = new Thread(reqMonitorRunnable);

        Random rand = new Random();
        // initialize list of ports 37000 - 37099 as selectable ports for cache servers to run on
        initializeSelectablePorts();
    }

    private void initializeSelectablePorts() {
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int _port) {
        port = _port;
    }

    @Override
    public void run() {
        Logger.log("CacheServer | Started CacheServer thread", Logger.LogType.THREAD_MANAGEMENT);
        // start server
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        HttpHandler clientReqHandler = new ClientRequestHandler(reqMonitor);
        HttpHandler capacityFactorRequestHandler = new CapacityFactorRequestHandler(reqMonitor);
        HttpServer server = null;

        for (int i = 0; i < selectablePorts.length; i++) {
            port = selectablePorts[i];
            Logger.log(String.format("attempting to start server on port %d\n", port), Logger.LogType.CACHE_SERVER_STARTUP);

            try {
                InetAddress host = InetAddress.getByName("127.0.0.1");
                InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                server = HttpServer.create(socketAddress, 0);
                server.createContext("/", clientReqHandler);
                server.createContext("/capacity-factor", capacityFactorRequestHandler);
                server.setExecutor(threadPoolExecutor);
                Logger.log(String.format("CacheServer | Server started on %s", socketAddress.toString()), Logger.LogType.CACHE_SERVER_STARTUP);
                break;
            } catch(IOException e) {
                Logger.log(String.format("CacheServer | Failed to start server on port %d", port), Logger.LogType.CACHE_SERVER_STARTUP);
            }
        }

        // start request telemetry curator
        reqMonitorThread.start();

        // start server
        server.start();
        Logger.log("Server started on port " + this.port, Logger.LogType.CACHE_SERVER_STARTUP);

        while(true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.log("CacheServer | CacheServer thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
                // shutdown associated telemetry curator thread
                reqMonitorThread.interrupt();
                Logger.log("CacheServer | Request Monitor thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
                server.stop(3);
                threadPoolExecutor.shutdown();
                break;
            }
        }

        Logger.log("CacheServer | Terminated CacheServer thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}
