package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.CapacityFactorMonitorFactory;
import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.services.monitor.CapacityFactorMonitor;
import loadbalancerlab.shared.RequestDecoderImpl;
import loadbalancerlab.shared.Logger;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.*;
import org.json.JSONObject;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LoadBalancer implements Runnable {
    private final int HASH_RING_DENOMINATIONS = 6_000;
    private final int DEFAULT_PORT = 3_000;

    private int port;
    private int cacheServerManagerPort;
    private List<HttpRequestInterceptor> requestInterceptors = new ArrayList<>();
    private List<HttpResponseInterceptor> responseInterceptors = new ArrayList<>();
    private HttpProcessor httpProcessor;
    // maps the port that cache server server is operating on to its capacity factor
    private CapacityFactorMonitor capacityFactorMonitor;
    private CapacityFactorMonitorFactory capacityFactorMonitorFactory;
    private Thread capacityFactorMonitorThread = null;
    private int startupServerCount;
    long initiationTime;
    private List<Integer> incomingRequestTimestamps;
    private ClientRequestHandler clientRequestHandler;
    private HttpClientFactory clientFactory;

    public LoadBalancer(int startupServerCount, int cacheServerManagerPort, CapacityFactorMonitorFactory capFactMonitorFact, HttpClientFactory clientFactory ) {
        // dummy port to indicate that the port has not been set
        this.cacheServerManagerPort = cacheServerManagerPort;
        this.port = -1;
        this.incomingRequestTimestamps = Collections.synchronizedList(new LinkedList<>());
        this.initiationTime = System.currentTimeMillis();
        this.startupServerCount = startupServerCount;
        this.clientRequestHandler = new ClientRequestHandler();
        this.httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
        this.capacityFactorMonitorFactory = capFactMonitorFact;
        this.clientFactory = clientFactory;
    }

    private class CapacityFactorMonitorRunnable implements Runnable {
        CapacityFactorMonitor capacityFactorMonitor;

        public CapacityFactorMonitorRunnable(CapacityFactorMonitor capFactorMonitor) {
            this.capacityFactorMonitor = capFactorMonitor;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    this.capacityFactorMonitor.pingServers(System.currentTimeMillis());
                    Thread.sleep(500);
                } catch(IOException e) {

                } catch(InterruptedException e) {
                    System.out.println("InterruptedException thrown in LoadBalancer#run");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            Logger.log("LoadBalancer | Terminated CapacityFactorMonitor thread", Logger.LogType.THREAD_MANAGEMENT);
        }
    }

    @Override
    public void run() {
        Logger.log("LoadBalancer | LoadBalancer thread started", Logger.LogType.THREAD_MANAGEMENT);
        InetAddress hostAddress = null;

        try {
            hostAddress = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException Within LoadBalancer#run");
            e.printStackTrace();
        }

        SocketConfig config = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        HttpServer server;
        int temporaryPort = DEFAULT_PORT;

        while (true) {
            server = ServerBootstrap.bootstrap()
                    .setLocalAddress(hostAddress)
                    .setListenerPort(temporaryPort)
                    .setHttpProcessor(httpProcessor)
                    .setSocketConfig(config)
                    .registerHandler("/api/*", this.clientRequestHandler)
                    .create();

            try {
                server.start();
            } catch (IOException e) {
                System.out.println("LoadBalancer | Failed to start server on port " + temporaryPort);
                temporaryPort++;
                continue;
            }

            // if server successfully started, exit the loop
            this.port = temporaryPort;
            break;
        }

        this.capacityFactorMonitor = this.capacityFactorMonitorFactory.produceCapacityFactorMonitor(new HttpClientFactoryImpl(), this.cacheServerManagerPort, new RequestDecoderImpl());
        CapacityFactorMonitorRunnable capacityFactorMonitorRunnable = new CapacityFactorMonitorRunnable(this.capacityFactorMonitor);
        capacityFactorMonitorThread = new Thread(capacityFactorMonitorRunnable);
        capacityFactorMonitorThread.start();
        startupCacheServerCluster();

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
            Logger.log("LoadBalancer | LoadBalancer thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
        } finally {
            server.shutdown(5, TimeUnit.SECONDS);
            // shut down capacity factor monitor thread
            capacityFactorMonitorThread.interrupt();
            // shut down this thread
            Thread.currentThread().interrupt();
            Logger.log("LoadBalancer | LoadBalancer thread terminated", Logger.LogType.THREAD_MANAGEMENT);
        }
    }

    public int getPort() {
        return this.port;
    }

    public SortedMap<Integer, Integer> deliverData() {
        SortedMap<Integer, Integer> output = new TreeMap<>();
        for (Integer timestamp : this.incomingRequestTimestamps) {
            if (output.containsKey(timestamp)) {
                int current = output.get(timestamp);
                output.put(timestamp, current + 1);
            } else {
                output.put(timestamp, 1);
            }
        }

        return output;
    }

    // REQUEST HANDLERS
    private class ClientRequestHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) {
            CloseableHttpClient httpClient = LoadBalancer.this.clientFactory.buildApacheClient();
            String uri = httpRequest.getRequestLine().getUri();
            String[] uriArr = uri.split("/", 0);
            int resourceId = Integer.parseInt(uriArr[uriArr.length - 1]);
            int cacheServerPort = LoadBalancer.this.capacityFactorMonitor.selectPort(resourceId);

            Logger.log(String.format("LoadBalancer | resourceId = %d", resourceId), Logger.LogType.REQUEST_PASSING);
            Logger.log(String.format("LoadBalancer | relaying message to cache server at port %d", cacheServerPort), Logger.LogType.REQUEST_PASSING);

            // record request incoming timestamp
            LoadBalancer.this.incomingRequestTimestamps.add((int)(System.currentTimeMillis() / 1000));
            HttpGet httpget = new HttpGet("http://127.0.0.1:" + cacheServerPort);

            try {
                CloseableHttpResponse response = httpClient.execute(httpget);
                HttpEntity responseBody = response.getEntity();
                httpResponse.setEntity(responseBody);
            } catch (IOException e) {
                // if request to cache server failed
                JSONObject outputJsonObj = new JSONObject();
                outputJsonObj.put("error_message", "Cache server failed to respond");
                String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
                InputStream stream = new ByteArrayInputStream(htmlResponse.getBytes());
                BasicHttpEntity responseBody = new BasicHttpEntity();
                responseBody.setContent(stream);
                httpResponse.setStatusCode(500);
                httpResponse.setEntity((HttpEntity)responseBody);
                System.out.println("LoadBalancer | IOException : Cache server failed to respond.");
            }
        }
    }

    // CACHE SERVER INITIALIZATION CODE
    private void startupCacheServerCluster() {
        int step = HASH_RING_DENOMINATIONS / this.startupServerCount;
        int hashRingIndex = 0;

        for (int i = 0; i < this.startupServerCount; i++) {
            this.capacityFactorMonitor.startupCacheServer(hashRingIndex);
            hashRingIndex += step;
        }
    }
}
