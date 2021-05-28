package loadbalancerlab.cacheserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import loadbalancerlab.services.monitor.RequestMonitor;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import loadbalancerlab.util.Logger;

public class CacheServer implements Runnable {
    final int TELEMETRY_CURATOR_RUNNING_TIME = 10_000;
    RequestMonitor reqMonitor;

    // class for periodically clearing out outdated telemetry
    private class TelemetryCurator implements Runnable {
        @Override
        public void run() {
            Logger.log("CacheServer | Started TelemetryCurator thread", Logger.LogType.THREAD_MANAGEMENT);
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() < startTime + TELEMETRY_CURATOR_RUNNING_TIME) {
                try {
                    Thread.sleep(300);
                    CacheServer.this.reqMonitor.clearOutData(System.currentTimeMillis());
                } catch (InterruptedException e) {
                    System.out.println("CacheServer Telemetry curator thread interrupted");
                }
            }
            Logger.log("CacheServer | Terminated TelemetryCurator thread", Logger.LogType.THREAD_MANAGEMENT);
        }
    }

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

    // http handler that is fed into HttpServer upon initialization
    // serves requests from load balancer that are from client
    private class CustomHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParams = extractParams(httpExchange);
            handleResponse(httpExchange, requestParams);
        }

        private void handleResponse(HttpExchange httpExchange, String requestParams) throws IOException {
            long startTime = System.currentTimeMillis();
            Logger.log("CacheServer | received request from load balancer", Logger.LogType.REQUEST_PASSING);
            try {
                Thread.sleep(200);
            } catch(InterruptedException e) {
                System.out.println("within CacheServer::CustomHandler.handleResponse");
                e.printStackTrace();
            }

            OutputStream outputStream = httpExchange.getResponseBody();

            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html>").append("<body>")
                    .append("<h1>")
                    .append("Hello")
                    .append("</h1>")
                    .append("</body>")
                    .append("</html>");

            // encode html content
            String htmlResponse = StringEscapeUtils.escapeHtml4(htmlBuilder.toString());

            // send out response
            httpExchange.sendResponseHeaders(200, htmlResponse.length());
            outputStream.write(htmlResponse.getBytes());
            Logger.log("CacheServer | sent request back to load balancer", Logger.LogType.REQUEST_PASSING);
            outputStream.flush();
            outputStream.close();
            long endTime = System.currentTimeMillis();
            CacheServer.this.reqMonitor.addRecord(startTime, endTime);
        }

        private String extractParams(HttpExchange httpExchange) {
            String[] intermediate1 = httpExchange.getRequestURI().toString().split("\\?");

            if (intermediate1.length > 1)
                return intermediate1[1];
            else
                return "";
        }
    }

    public volatile int port;
    int[] selectablePorts = new int[100];

    public CacheServer(RequestMonitor reqMonitor) {
        this.reqMonitor = reqMonitor;
        Random rand = new Random();
        // initialize list of ports 37000 - 37099 as selectable ports for cache servers to run on
        initializeSelectablePorts();
    }

    private void initializeSelectablePorts() {
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;
    }

    @Override
    public void run() {
        Logger.log("CacheServer | Started CacheServer thread", Logger.LogType.THREAD_MANAGEMENT);
        // start server
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        HttpHandler customHttpHandler = new CustomHttpHandler();
        HttpHandler capacityFactorRequestHandler = new CapacityFactorRequestHandler();
        HttpServer server = null;

        for (int i = 0; i < selectablePorts.length; i++) {
            port = selectablePorts[i];
            Logger.log(String.format("attempting to start server on port %d\n", port), Logger.LogType.CACHE_SERVER_STARTUP);

            try {
                InetAddress host = InetAddress.getByName("127.0.0.1");
                InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                server = HttpServer.create(socketAddress, 0);
                server.createContext("/", customHttpHandler);
                server.createContext("/capacity-factor", capacityFactorRequestHandler);
                server.setExecutor(threadPoolExecutor);
                Logger.log(String.format("CacheServer | Server started on %s", socketAddress.toString()), Logger.LogType.CACHE_SERVER_STARTUP);
                break;
            } catch(IOException e) {
                Logger.log(String.format("CacheServer | Failed to start server on port %d", port), Logger.LogType.CACHE_SERVER_STARTUP);
            }
        }

        // start request telemetry curator
        Thread telemetryCuratorThread = new Thread(new TelemetryCurator());
        telemetryCuratorThread.start();

        // start server
        server.start();
        Logger.log("Server started on port " + this.port, Logger.LogType.CACHE_SERVER_STARTUP);

        while(true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Logger.log("CacheServer | CacheServer thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
                Thread.currentThread().interrupt();
                server.stop(3);
                threadPoolExecutor.shutdown();
                break;
            }
        }

        Logger.log("CacheServer | Terminated CacheServer thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}
