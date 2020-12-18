import org.apache.commons.io.IOUtils;
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

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class LoadBalancer implements Runnable {
    int port;
    List<HttpRequestInterceptor> requestInterceptors = new ArrayList<>();
    List<HttpResponseInterceptor> responseInterceptors = new ArrayList<>();
    HttpProcessor httpProcessor;
    Map<BackEnd.Type, List<Integer>> backendPortIndex = new HashMap<>();
    ConcurrentMap<Integer, Double> capacityFactors;
    private static final int BACKEND_INITIATOR_PORT = 3000;
    private static final int STARTUP_BACKEND_DYNO_COUNT = 4;
    Random rand;

    public LoadBalancer(int port) {
        this.port = port;
        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
        backendPortIndex.put(BackEnd.Type.HOME_PAGE_SERVER, new ArrayList<>());
        backendPortIndex.put(BackEnd.Type.IMAGE_FILE_SERVER, new ArrayList<>());
        rand = new Random();
        capacityFactors = new ConcurrentHashMap<>();
    }

    class CapacityFactorCalculator implements Runnable {
        public CapacityFactorCalculator() {

        }

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(500);
                } catch(InterruptedException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("=====================================");
                System.out.println("capacity factor calculator running");
                System.out.println("=====================================");
                System.out.println("capacity factors = " + capacityFactors);
            }
        }
    }

    // stores information about handled requests
    class RequestAnalytics {
        long processingTime, startTime, endTime;

        public RequestAnalytics(long startTime, long endTime) {
            this.processingTime = endTime - startTime;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String toString() {
            return String.format("processing time : %d | start time: %d", processingTime, startTime);
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
                    .registerHandler("/image", new ClientImageRequestHandler())
                    .registerHandler("/home", new ClientHomeRequestHandler())
                    .create();

            server.start();
            startupBackendCluster();
            Thread capacityFactorMonitor = new Thread(new CapacityFactorCalculator());
            capacityFactorMonitor.start();
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
    private class ClientImageRequestHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws IOException {
            int backendPort = selectPort(BackEnd.Type.IMAGE_FILE_SERVER);
            CloseableHttpClient httpClient = HttpClients.createDefault();
            Logger.log(String.format("LoadBalancer | relaying message to image file server at port %d", backendPort));
            HttpGet httpget = new HttpGet("http://127.0.0.1:" + backendPort);
            CloseableHttpResponse response = httpClient.execute(httpget);

            HttpEntity responseBody = response.getEntity();
            httpResponse.setEntity(responseBody);
        }
    }

    private class ClientHomeRequestHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws IOException {
            int backendPort = selectPort(BackEnd.Type.HOME_PAGE_SERVER);
            CloseableHttpClient httpClient = HttpClients.createDefault();
            Logger.log(String.format("LoadBalancer | relaying message to home server at port %d", backendPort));
            HttpGet httpget = new HttpGet("http://127.0.0.1:" + backendPort);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity responseBody = response.getEntity();
            httpResponse.setEntity(responseBody);
        }
    }

    // BACKEND INITIALIZATION CODE
    private void startupBackendCluster() {
        for (int i = 0; i < STARTUP_BACKEND_DYNO_COUNT; i++) {
            if (i % 2 == 0) {
                startupBackend(BackEnd.Type.IMAGE_FILE_SERVER);
            } else {
                startupBackend(BackEnd.Type.HOME_PAGE_SERVER);
            }
        }
    }

    private void startupBackend(BackEnd.Type type) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://127.0.0.1:" + BACKEND_INITIATOR_PORT + "/backend/start");
        List<NameValuePair> params = new ArrayList<>();

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
                Logger.log("LoadBalancer | new backend port = " + responseString);
                int portInt = Integer.valueOf(responseString);
                backendPortIndex.get(type).add(portInt);
                capacityFactors.put(portInt, -1.0);
                Logger.log("LoadBalancer | backend ports:");
                System.out.println("LoadBalancer | backend ports:");
                for (Map.Entry<BackEnd.Type, List<Integer>> entry : backendPortIndex.entrySet()) {
                    Logger.log(String.format("%s : %s", entry.getKey(), entry.getValue()));
                }
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
    }

    // PRIVATE HELPER METHODS
    private int selectPort(BackEnd.Type type) {
        List<Integer> availablePorts = backendPortIndex.get(type);
        return availablePorts.get(rand.nextInt(availablePorts.size()));
    }

    public static void main(String[] args) {
        new LoadBalancer(8080).run();
    }
}
