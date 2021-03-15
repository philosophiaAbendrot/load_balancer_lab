import loadbalancer.monitor.RequestMonitor;
import loadbalancer.util.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ImmutableHttpProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import loadbalancer.monitor.ServerMonitor;

public class BackEndInitiator implements Runnable {
    public static final int DEFAULT_PORT = 8000;
    private int port;
    private List<HttpRequestInterceptor> requestInterceptors = new ArrayList<HttpRequestInterceptor>();
    private List<HttpResponseInterceptor> responseInterceptors = new ArrayList<HttpResponseInterceptor>();
    private Map<Integer, Thread> portsToBackendThreads = new ConcurrentHashMap<>();
    private HttpProcessor httpProcessor;
    private int[] selectablePorts = new int[100];
    private ServerMonitorRunnable serverMonitorRunnable;

    public BackEndInitiator() {
        abstractConstructor();
    }

    private void abstractConstructor() {
        this.port = -1;

        // reserve ports 37000 through 37099 as usable ports
        for (int i = 0; i < selectablePorts.length; i++) {
            selectablePorts[i] = 37100 + i;
        }

        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
    }

    @Override
    public void run() {
        Logger.log("BackEndInitiator | Started BackendInitiator thread", "threadManagement");
        InetAddress hostAddress = null;
        this.serverMonitorRunnable = new ServerMonitorRunnable(new ServerMonitor());
        Thread serverMonitorThread = new Thread(serverMonitorRunnable);
        serverMonitorThread.start();

        try {
            hostAddress = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException within BackendInitiator#run");
            e.printStackTrace();
        }

        SocketConfig config = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        HttpServer server;

        int chosenPort = DEFAULT_PORT;

        while (true) {
            System.out.println("Attempting to start server on port " + chosenPort);
            try {
                server = ServerBootstrap.bootstrap()
                        .setLocalAddress(hostAddress)
                        .setListenerPort(chosenPort)
                        .setHttpProcessor(httpProcessor)
                        .setSocketConfig(config)
                        .registerHandler("/backends", new ServerStartHandler())
                        .registerHandler("/backend/*", new ServerUpdateHandler())
                        .create();

                server.start();
            } catch(IOException e) {
                System.out.println("IOException within BackEndInitiator#run");
                System.out.println("Failed to start server on selected port. Trying another port");
                chosenPort++;
                continue;
            }

            // break out of loop if server successfully started
            System.out.println("BackEndInitiator | Server successfully started on port " + chosenPort);
            break;
        }

        this.port = chosenPort;

        try {
            HttpServer finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    finalServer.shutdown(5, TimeUnit.SECONDS);
                }
            });

            server.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.log("BackEndInitiator | BackEndInitiator thread interrupted", "threadManagement");
        } finally {
            // shutdown server
            server.shutdown(5, TimeUnit.SECONDS);
            // shutdown all backend servers spawned by this server
            for (Map.Entry<Integer, Thread> entry : this.portsToBackendThreads.entrySet()) {
                Thread backendThread = entry.getValue();
                int threadId = (int)backendThread.getId();
                backendThread.interrupt();
                Logger.log("BackEndInitiator | Terminating backend thread " + threadId, "threadManagement");
            }

            // terminate server monitor thread
            serverMonitorThread.interrupt();
            Logger.log("BackEndInitiator | Terminated server monitor thread", "threadManagement");
            Thread.currentThread().interrupt();
            Logger.log("BackEndInitiator | Terminated BackEndInitiator thread", "threadManagement");
        }
    }

    public int getPort() {
        return this.port;
    }

    public Set<Map.Entry<Integer, Integer>> deliverData() {
        return this.serverMonitorRunnable.deliverData();
    }

    private class ServerMonitorRunnable implements Runnable {
        ServerMonitor serverMonitor;

        public ServerMonitorRunnable(ServerMonitor serverMonitor) {
            this.serverMonitor = serverMonitor;
        }

        @Override
        public void run() {
            Logger.log("BackendInitiator | Starting ServerMonitor", "threadManagement");

            while (true) {
                try {
                    Thread.sleep(100);
                    int currentSecond = (int)(System.currentTimeMillis() / 1000);
                    this.serverMonitor.addRecord(currentSecond, BackEndInitiator.this.portsToBackendThreads.size());
                } catch (InterruptedException e) {
                    Logger.log("BackEndInitiator | Shutting down ServerMonitor", "threadManagement");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public Set<Map.Entry<Integer, Integer>> deliverData() {
            return serverMonitor.deliverData();
        }
    }

    private class ServerStartHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws IOException {
            Logger.log("BackendInitiator | received backend initiate request", "capacityModulation");
            BackEnd backend = new BackEnd(new RequestMonitor("BackEnd"));
            Thread backendThread = new Thread(backend);
            Logger.log("BackendInitiator | started backend thread", "capacityModulation");
            backendThread.start();

            // wait for backend port to be selected by backend
            int tryNumber = 1;
            while (backend.port == 0) {
                // check periodically for the backend port
                try {
                    Thread.sleep(20);
                } catch(InterruptedException e) {
                    Logger.log("BackEndInitiator | initiateRequestHandler thread interrupted", "threadManagement");
                }
            }

            BackEndInitiator.this.portsToBackendThreads.put(backend.port, backendThread);
            Logger.log("chosen backend port = " + backend.port, "capacityModulation");
            BasicHttpEntity responseEntity = new BasicHttpEntity();
            InputStream responseStream = IOUtils.toInputStream(String.valueOf(backend.port), StandardCharsets.UTF_8.name());
            responseEntity.setContent(responseStream);
            responseStream.close();
            httpResponse.setEntity(responseEntity);
        }
    }

    private class ServerUpdateHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) {
            String method = httpRequest.getRequestLine().getMethod();
            String uri = httpRequest.getRequestLine().getUri();
            String[] parsedUri = uri.split("/");
            int port = Integer.valueOf(parsedUri[parsedUri.length - 1]);
            Logger.log("BackEndInitiator | received request to shutdown backend on port " + port, "capacityModulation");

            if (method.equals("DELETE")) {
                BackEndInitiator.this.portsToBackendThreads.get(port).interrupt();
                BackEndInitiator.this.portsToBackendThreads.remove(port);
                Logger.log("BackEndInitiator | Shut down backend server running on port " + port, "capacityModulation");
            }
        }
    }
}
