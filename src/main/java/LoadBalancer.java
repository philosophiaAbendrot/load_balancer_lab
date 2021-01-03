import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.*;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class LoadBalancer implements Runnable {
    private final int HASH_RING_DENOMINATIONS = 60;

    int port;
    List<HttpRequestInterceptor> requestInterceptors = new ArrayList<>();
    List<HttpResponseInterceptor> responseInterceptors = new ArrayList<>();
    HttpProcessor httpProcessor;
    Map<Integer, Integer> backendPortIndex = new HashMap<>();
    ConcurrentMap<Integer, Double> capacityFactors;
    private static final int BACKEND_INITIATOR_PORT = 3000;
    private static final int STARTUP_BACKEND_DYNO_COUNT = 4;
    Random rand;

    public LoadBalancer(int port) {
        this.port = port;
        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
        rand = new Random();
        capacityFactors = new ConcurrentHashMap<>();
        Thread capacityFactorMonitor = new Thread(new CapacityFactorMonitor());
        capacityFactorMonitor.start();
    }

    class CapacityFactorMonitor implements Runnable {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(500);

                    for (Map.Entry<Integer, Double> entry : capacityFactors.entrySet()) {
                        int backendPort = entry.getKey();
                        CloseableHttpClient httpClient = HttpClients.createDefault();
                        Logger.log(String.format("LoadBalancer | sending request for update on capacity factor to port %d", backendPort));
                        HttpGet httpget = new HttpGet("http://127.0.0.1:" + backendPort + "/capacity_factor");
                        try {
                            CloseableHttpResponse response = httpClient.execute(httpget);
                            HttpEntity responseBody = response.getEntity();
                            InputStream responseStream = responseBody.getContent();
                            String responseString = IOUtils.toString(responseStream, StandardCharsets.UTF_8.name());
                            responseStream.close();
                            JSONObject responseJson = new JSONObject(StringEscapeUtils.unescapeJson(responseString));
                            double capacityFactor = responseJson.getDouble("capacity_factor");
                            Logger.log(String.format("LoadBalancer | received update on capacity factor: %s", capacityFactor));
                            entry.setValue(capacityFactor);
                            httpClient.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch(InterruptedException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("=====================================");
                System.out.println("capacity factor monitor running");
                System.out.println("=====================================");
                System.out.println("capacity factors = " + capacityFactors);
            }
        }
    }

    @Override
    public void run() {
        SocketConfig config = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();
        try {
            InetAddress hostAddress = InetAddress.getByName("127.0.0.1");
            final HttpServer server = ServerBootstrap.bootstrap()
                    .setLocalAddress(hostAddress)
                    .setListenerPort(port)
                    .setHttpProcessor(httpProcessor)
                    .setSocketConfig(config)
                    .registerHandler("/api/*", new ClientRequestHandler())
                    .create();

            server.start();
            startupBackendCluster();
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.shutdown(5, TimeUnit.SECONDS);
                }
            });
        } catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // REQUEST HANDLERS
    private class ClientRequestHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws IOException {
            int backendPort = selectPort();
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String uri = httpRequest.getRequestLine().getUri();
            String[] uriArr = uri.split("/", 0);
            int resourceId = Integer.parseInt(uriArr[uriArr.length - 1]);
            Logger.log(String.format("LoadBalancer | resourceId = %d", resourceId));
            Logger.log(String.format("LoadBalancer | relaying message to image file server at port %d", backendPort));
            HttpGet httpget = new HttpGet("http://127.0.0.1:" + backendPort);
            CloseableHttpResponse response = httpClient.execute(httpget);

            HttpEntity responseBody = response.getEntity();
            httpResponse.setEntity(responseBody);
        }
    }

    // BACKEND INITIALIZATION CODE
    private void startupBackendCluster() {
        int step = HASH_RING_DENOMINATIONS / STARTUP_BACKEND_DYNO_COUNT;
        int hashRingIndex = 0;

        for (int i = 0; i < STARTUP_BACKEND_DYNO_COUNT; i++) {
            int portInt = startupBackend();
            backendPortIndex.put(hashRingIndex, portInt);
            hashRingIndex += step;
        }
    }

    private int startupBackend() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://127.0.0.1:" + BACKEND_INITIATOR_PORT + "/backend/start");
        List<NameValuePair> params = new ArrayList<>();
        int portInt = -1;

        while(true) {
            try {
                Thread.sleep(100);
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                Logger.log("LoadBalancer | sent request to startup a backend");
                CloseableHttpResponse response = httpClient.execute(httpPost);
                Logger.log("Load Balancer | received response");
                HttpEntity responseBody = response.getEntity();
                InputStream responseStream = responseBody.getContent();
                String responseString = IOUtils.toString(responseStream, StandardCharsets.UTF_8.name());
                responseStream.close();
                httpClient.close();
                Logger.log("LoadBalancer | new backend port = " + responseString);
                portInt = Integer.valueOf(responseString);
                capacityFactors.put(portInt, -1.0);
                Logger.log("LoadBalancer | backend ports:");
                System.out.println("LoadBalancer | backend ports:");

                for (Map.Entry<Integer, Integer> entry : backendPortIndex.entrySet())
                    Logger.log(String.format("LoadBalancer | Index: %s | Port: %s", entry.getKey(), entry.getValue()));

                break;
            } catch (UnsupportedEncodingException | UnsupportedOperationException | ClientProtocolException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }

        return portInt;
    }

    // PRIVATE HELPER METHODS
    private int selectPort() {
        List<Integer> ports = new ArrayList<>(backendPortIndex.values());
        return ports.get(rand.nextInt(backendPortIndex.size()));
    }

    public static void main(String[] args) {
        new LoadBalancer(8080).run();
    }
}
