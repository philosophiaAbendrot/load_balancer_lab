package loadbalancerlab.cacheservermanager;

import loadbalancerlab.services.monitor.RequestMonitor;
import loadbalancerlab.cacheserver.CacheServer;
import loadbalancerlab.shared.ServerInfo;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.factory.CacheServerFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.RequestDecoder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
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

import org.json.JSONObject;

public class CacheServerManager implements Runnable {
    public static final int DEFAULT_PORT = 8000;
    private int port;
    Map<Integer, Thread> portsToServerThreads = new ConcurrentHashMap<>();
    private HttpProcessor httpProcessor;
    private int[] selectablePorts = new int[100];
    private CacheServerFactory cacheServerFactory;
    private HttpClientFactory clientFactory;
    public RequestDecoder reqDecoder;
    private ServerMonitorRunnable serverMonitor;

    public CacheServerManager( CacheServerFactory cacheServerFactory, HttpClientFactory clientFactory, RequestDecoder reqDecoder) {
        this.port = -1;
        this.cacheServerFactory = cacheServerFactory;
        this.clientFactory = clientFactory;
        this.reqDecoder = reqDecoder;

        // reserve ports 37000 through 37099 as usable ports
        for (int i = 0; i < selectablePorts.length; i++)
            this.selectablePorts[i] = 37100 + i;

        this.httpProcessor = new ImmutableHttpProcessor(new ArrayList<>(), new ArrayList<>());
        this.serverMonitor = new ServerMonitorRunnable(clientFactory, reqDecoder, this);
    }

    @Override
    public void run() {
        Logger.log("CacheServerManager | Started CacheServerManager thread", Logger.LogType.PRINT_NOTHING);
        InetAddress hostAddress = null;
        Thread serverMonitorThread = new Thread(this.serverMonitor);
        serverMonitorThread.start();

        try {
            hostAddress = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException within CacheServerManager#run");
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
                        .registerHandler("/cache-servers", new CacheInfoRequestHandler(this))
                        .create();

                server.start();
            } catch(IOException e) {
                System.out.println("IOException within CacheServerManager#run");
                System.out.println("Failed to start server on selected port. Trying another port");
                chosenPort++;
                continue;
            }

            // break out of loop if server successfully started
            System.out.println("CacheServerManager | Server successfully started on port " + chosenPort);
            break;
        }

        this.port = chosenPort;

        HttpServer finalServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                finalServer.shutdown(5, TimeUnit.SECONDS);
            }
        });

        try {
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.log("CacheServerManager | CacheServerManager thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
        } finally {
            // shutdown server
            server.shutdown(5, TimeUnit.SECONDS);
            // shutdown all cache servers spawned by this server
            for (Map.Entry<Integer, Thread> entry : this.portsToServerThreads.entrySet()) {
                Thread serverThread = entry.getValue();
                int threadId = (int)serverThread.getId();
                serverThread.interrupt();
                Logger.log("CacheServerManager | Terminating CacheServer thread " + threadId, Logger.LogType.THREAD_MANAGEMENT);
            }

            // terminate server monitor thread
            serverMonitorThread.interrupt();
            Logger.log("CacheServerManager | Terminated server monitor thread", Logger.LogType.THREAD_MANAGEMENT);
            Thread.currentThread().interrupt();
            Logger.log("CacheServerManager | Terminated CacheServerManager thread", Logger.LogType.THREAD_MANAGEMENT);
        }
    }

    public int getPort() {
        return this.port;
    }

    public SortedMap<Integer, Integer> deliverData() {
        return this.serverMonitor.deliverData();
    }

    public void startupCacheServer(int num) {
        return;
    }

    public void shutdownCacheServer(int num) {
        return;
    }

    int numServers() {
        return this.portsToServerThreads.size();
    }

    Map<Integer, ServerInfo> getServerInfo() {
        return serverMonitor.getServerInfo();
    }

//    private class ServerStartHandler implements HttpRequestHandler {
//        @Override
//        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws IOException {
//            Logger.log("CacheServerManager | received cache server initiate request", Logger.LogType.CAPACITY_MODULATION);
//            CacheServer cacheServer = CacheServerManager.this.cacheServerFactory.produceCacheServer(new RequestMonitor("CacheServer"));
//            Thread cacheServerThread = CacheServerManager.this.cacheServerFactory.produceCacheServerThread(cacheServer);
//            Logger.log("CacheServerManager | started cache server thread", Logger.LogType.CAPACITY_MODULATION);
//            cacheServerThread.start();
//
//            // wait for port to be selected by cache server
//            while (cacheServer.port == 0) {
//                // check periodically for the cache server port
//                try {
//                    Thread.sleep(20);
//                } catch(InterruptedException e) {
//                    Logger.log("CacheServerManager | initiateRequestHandler thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
//                }
//            }
//
//            CacheServerManager.this.portsToServerThreads.put(cacheServer.port, cacheServerThread);
//            Logger.log("chosen cache server port = " + cacheServer.port, Logger.LogType.CAPACITY_MODULATION);
//
//            JSONObject outputJsonObj = new JSONObject();
//            outputJsonObj.put("port", cacheServer.port);
//            String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
//            BasicHttpEntity responseEntity = new BasicHttpEntity();
//            InputStream responseStream = IOUtils.toInputStream(String.valueOf(htmlResponse), StandardCharsets.UTF_8.name());
//            responseEntity.setContent(responseStream);
//            responseStream.close();
//            httpResponse.setEntity(responseEntity);
//        }
//    }
}
