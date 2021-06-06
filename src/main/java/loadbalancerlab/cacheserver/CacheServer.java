package loadbalancerlab.cacheserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
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

    // http handler that is fed into HttpServer upon initialization
    // serves direct requests from load balancer for updates on capacity factor
    private class CapacityFactorRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            OutputStream outputStream = httpExchange.getResponseBody();

            double capacityFactor = CacheServer.this.reqMonitor.getCapacityFactor(System.currentTimeMillis());

            Logger.log(String.format("CacheServer | capacityFactor = %f", capacityFactor), Logger.LogType.REQUEST_PASSING);

            JSONObject outputJsonObj = new JSONObject();
            outputJsonObj.put("capacity_factor", capacityFactor);

            // encode html content
            String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
            Logger.log("CacheServer | CapacityFactorRequestHandler processed request", Logger.LogType.REQUEST_PASSING);
            // send out response
            httpExchange.sendResponseHeaders(200, htmlResponse.length());
            outputStream.write(htmlResponse.getBytes());
            outputStream.flush();
            outputStream.close();
        }
    }

    private volatile int port;
    int[] selectablePorts = new int[100];

    public CacheServer(RequestMonitor reqMonitor) {
        reqMonitor = reqMonitor;
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
        HttpHandler capacityFactorRequestHandler = new CapacityFactorRequestHandler();
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
                server.stop(3);
                threadPoolExecutor.shutdown();
                break;
            }
        }

        Logger.log("CacheServer | Terminated CacheServer thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}
