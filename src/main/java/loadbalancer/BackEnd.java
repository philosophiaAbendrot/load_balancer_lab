package loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import loadbalancer.monitor.RequestMonitor;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import loadbalancer.util.Logger;

public class BackEnd implements Runnable {
    final int TELEMETRY_CURATOR_RUNNING_TIME = 10_000;
    final int BACKEND_RUNNING_TIME = 15_000;
    RequestMonitor reqMonitor;

    // class for periodically clearing out outdated telemetry
    private class TelemetryCurator implements Runnable {
        @Override
        public void run() {
            Logger.log("BackEnd | Started TelemetryCurator thread", "threadManagement");
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() < startTime + TELEMETRY_CURATOR_RUNNING_TIME) {
                try {
                    Thread.sleep(300);
                    BackEnd.this.reqMonitor.clearOutData(System.currentTimeMillis());
                } catch (InterruptedException e) {
                    System.out.println("Backend Telemetry curator thread interrupted");
                }
            }
            Logger.log("BackEnd | Terminated TelemetryCurator thread", "threadManagement");
        }
    }

    // http handler that is fed into HttpServer upon initialization
    // serves direct requests from load balancer for updates on capacity factor
    private class CapacityFactorRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            OutputStream outputStream = httpExchange.getResponseBody();

            double capacityFactor = BackEnd.this.reqMonitor.getCapacityFactor(System.currentTimeMillis());

            Logger.log(String.format("Backend | capacityFactor = %f", capacityFactor), "requestPassing");

            JSONObject outputJsonObj = new JSONObject();
            outputJsonObj.put("capacity_factor", capacityFactor);

            // encode html content
            String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
            Logger.log("BackEnd | CapacityFactorRequestHandler processed request", "requestPassing");
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
            Logger.log("BackEnd | received request from load balancer", "requestPassing");
            try {
                Thread.sleep(200);
            } catch(InterruptedException e) {
                System.out.println("within Backend::CustomHandler.handleResponse");
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
            Logger.log("BackEnd | sent request back to load balancer", "requestPassing");
            outputStream.flush();
            outputStream.close();
            long endTime = System.currentTimeMillis();
            BackEnd.this.reqMonitor.addRecord(startTime, endTime);
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

    public BackEnd(RequestMonitor reqMonitor) {
        this.reqMonitor = reqMonitor;
        Random rand = new Random();
        // initialize list of ports 37000 - 37099 as selectable ports for backend server to run on
        initializeSelectablePorts();
    }

    private void initializeSelectablePorts() {
        for (int i = 0; i < selectablePorts.length; i++)
            selectablePorts[i] = 37100 + i;
    }

    @Override
    public void run() {
        Logger.log("BackEnd | Started BackEnd thread", "threadManagement");
        // start server
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        HttpHandler customHttpHandler = new CustomHttpHandler();
        HttpHandler capacityFactorRequestHandler = new CapacityFactorRequestHandler();
        HttpServer server = null;

        for (int i = 0; i < selectablePorts.length; i++) {
            port = selectablePorts[i];
            Logger.log(String.format("attempting to start server on port %d\n", port), "backendStartup");

            try {
                InetAddress host = InetAddress.getByName("127.0.0.1");
                InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                server = HttpServer.create(socketAddress, 0);
                server.createContext("/", customHttpHandler);
                server.createContext("/capacity_factor", capacityFactorRequestHandler);
                server.setExecutor(threadPoolExecutor);
                Logger.log(String.format("BackEnd | Server started on %s", socketAddress.toString()), "backendStartup");
                break;
            } catch(IOException e) {
                Logger.log(String.format("BackEnd | Failed to start server on port %d", port), "backendStartup");
            }
        }

        // start request telemetry curator
        Thread telemetryCuratorThread = new Thread(new TelemetryCurator());
        telemetryCuratorThread.start();

        // start server
        server.start();
        Logger.log("Server started on port " + this.port, "backendStartup");

        while(true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Logger.log("BackEnd | BackEnd thread interrupted", "threadManagement");
                Thread.currentThread().interrupt();
                server.stop(3);
                threadPoolExecutor.shutdown();
                break;
            }
        }

        Logger.log("BackEnd | Terminated BackEnd thread", "threadManagement");
    }
}
