import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
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
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class LoadBalancer implements Runnable {
    private final int HASH_RING_DENOMINATIONS = 60;
    private final double CAPACITY_FACTOR_MAX = 0.3;
    private final int REST_INTERVAL = 5_000;
    private final int REINFORCEMENT_INTERVAL = 5_000;

    int port;
    List<HttpRequestInterceptor> requestInterceptors = new ArrayList<>();
    List<HttpResponseInterceptor> responseInterceptors = new ArrayList<>();
    Map<Integer, Long> reinforcedTimes = new HashMap<>(); // holds a map of when each backend port was last reinforced
    HttpProcessor httpProcessor;
    Map<Integer, Integer> backendPortIndex = new HashMap<>();
    ConcurrentMap<Integer, Double> capacityFactors;
    Thread capacityFactorMonitorThread = null;
    private static final int BACKEND_INITIATOR_PORT = 3000;
    private static final int STARTUP_BACKEND_DYNO_COUNT = 1;
    private int startupServerCount;
    Random rand;
    long initiationTime;
    List<Integer> incomingRequestTimestamps;
    ClientRequestHandler clientRequestHandler;

    public LoadBalancer(int port, int startupServerCount) {
        this.port = port;
        this.incomingRequestTimestamps = Collections.synchronizedList(new LinkedList<>());
        this.initiationTime = System.currentTimeMillis();
        this.startupServerCount = startupServerCount;
        this.clientRequestHandler = new ClientRequestHandler();
        httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);
        rand = new Random();
        capacityFactors = new ConcurrentHashMap<>();
    }

    class CapacityFactorMonitor implements Runnable {
        @Override
        public void run() {
            Logger.log("LoadBalancer | Started CapacityFactorMonitor thread", "threadManagement");
            while(true) {
                try {
                    // update capacity factors every 0.5s by pinging each backend
                    Thread.sleep(500);

                    for (Map.Entry<Integer, Double> entry : capacityFactors.entrySet()) {
                        int backendPort = entry.getKey();
                        CloseableHttpClient httpClient = HttpClients.createDefault();
                        Logger.log(String.format("LoadBalancer | sending request for update on capacity factor to port %d", backendPort), "telemetryUpdate");
                        HttpGet httpget = new HttpGet("http://127.0.0.1:" + backendPort + "/capacity_factor");
                        try {
                            CloseableHttpResponse response = httpClient.execute(httpget);
                            HttpEntity responseBody = response.getEntity();
                            InputStream responseStream = responseBody.getContent();

                            String responseString = IOUtils.toString(responseStream, StandardCharsets.UTF_8.name());
                            JSONObject responseJson = new JSONObject(StringEscapeUtils.unescapeJson(responseString));
                            double capacityFactor = responseJson.getDouble("capacity_factor");
                            Logger.log(String.format("LoadBalancer | received update on capacity factor: %s", capacityFactor), "telemetryUpdate");
                            entry.setValue(capacityFactor);

                            responseStream.close();
                            httpClient.close();

                            if (System.currentTimeMillis() > initiationTime + REST_INTERVAL && capacityFactor > CAPACITY_FACTOR_MAX) {
                                if (reinforcedTimes.containsKey(backendPort)) {
                                    // if a server has been started up to reinforce backendPort recently
                                    long lastReinforced = reinforcedTimes.get(backendPort);
                                    Logger.log(String.format("Load Balancer | last reinforced = %d", lastReinforced), "capacityModulation");

                                    if (System.currentTimeMillis() > lastReinforced + REINFORCEMENT_INTERVAL) {
                                        // if the backend port was reinforced a while ago, clear it out from the record
                                        Logger.log(String.format("Load Balancer | clearing backendPort %d from reinforcedTimes", backendPort), "capacityModulation");
                                        reinforcedTimes.remove(backendPort);
                                    } else {
                                        // if the backend port was reinforced recently, do not reinforce it again
                                        Logger.log(String.format("Load Balancer | skipping reinforcement of port %d", backendPort), "capacityModulation");
                                        continue;
                                    }
                                }

                                // startup a new dyno
                                Logger.log(String.format("Load Balancer | backendPort %d is overloaded with cf = %f", backendPort, capacityFactor), "capacityModulation");
                                int newServerHashRingLocation = selectHashRingLocation(backendPort);
                                Logger.log(String.format("Load Balancer | selected location %d for new server", newServerHashRingLocation), "capacityModulation");
                                // start a new server at the new hash ring location
                                int newServerPort = startupBackend();
                                // record location of new dyno along with port
                                backendPortIndex.put(newServerHashRingLocation, newServerPort);
                                // record that backendPort was reinforced
                                reinforcedTimes.put(backendPort, System.currentTimeMillis());
                            }
                        } catch(IOException e) {
                            System.out.println("IOException thrown in LoadBalancer::CapacityFactorMonitor#run");
                            e.printStackTrace();
                        }
                    }
                } catch(InterruptedException e) {
                    System.out.println("InterruptedException thrown in LoadBalancer#run");
                    e.printStackTrace();
                    System.out.println("isInterrupted = " + Thread.currentThread().isInterrupted());
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            Logger.log("LoadBalancer | Terminated CapacityFactorMonitor thread", "threadManagement");
        }
    }

    @Override
    public void run() {
        Logger.log("LoadBalancer | LoadBalancer thread started", "threadManagement");
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

        final HttpServer server = ServerBootstrap.bootstrap()
                .setLocalAddress(hostAddress)
                .setListenerPort(port)
                .setHttpProcessor(httpProcessor)
                .setSocketConfig(config)
                .registerHandler("/api/*", this.clientRequestHandler)
                .create();

        try {
            server.start();
            startupBackendCluster();
            capacityFactorMonitorThread = new Thread(new CapacityFactorMonitor());
            capacityFactorMonitorThread.start();
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.shutdown(5, TimeUnit.SECONDS);
                }
            });
        } catch (IOException e) {
            System.out.println("IOException within LoadBalancer#run");
            e.printStackTrace();
        } catch (InterruptedException e) {
            server.shutdown(5, TimeUnit.SECONDS);
            Logger.log("LoadBalancer | LoadBalancer thread interrupted", "threadManagement");
            // shut down capacity factor monitor thread
            capacityFactorMonitorThread.interrupt();
            Thread.currentThread().interrupt();
        }

        Logger.log("LoadBalancer | LoadBalancer thread terminated", "threadManagement");
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
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws IOException {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            String uri = httpRequest.getRequestLine().getUri();
            String[] uriArr = uri.split("/", 0);
            int resourceId = Integer.parseInt(uriArr[uriArr.length - 1]);
            int backendPort = selectPort(resourceId);

            Logger.log(String.format("LoadBalancer | resourceId = %d", resourceId), "requestPassing");
            Logger.log(String.format("LoadBalancer | relaying message to backend server at port %d", backendPort), "requestPassing");

            // record request incoming timestamp
            LoadBalancer.this.incomingRequestTimestamps.add((int)(System.currentTimeMillis() / 1000));

            HttpGet httpget = new HttpGet("http://127.0.0.1:" + backendPort);

            CloseableHttpResponse response = httpClient.execute(httpget);

            HttpEntity responseBody = response.getEntity();
            httpResponse.setEntity(responseBody);
        }
    }

    // BACKEND INITIALIZATION CODE
    private void startupBackendCluster() {
        int step = HASH_RING_DENOMINATIONS / this.startupServerCount;
        int hashRingIndex = 0;

        for (int i = 0; i < this.startupServerCount; i++) {
            int portInt = startupBackend();
            backendPortIndex.put(hashRingIndex, portInt);
            hashRingIndex += step;
        }
    }

    private int startupBackend() {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("http://127.0.0.1:" + BACKEND_INITIATOR_PORT + "/backend/start");

        int portInt = -1;

        while(true) {
            try {
                Thread.sleep(100);
                Logger.log("LoadBalancer | sent request to startup a backend", "loadBalancerStartup");
                CloseableHttpResponse response = httpClient.execute(httpPost);

                Logger.log("Load Balancer | received response", "loadBalancerStartup");
                HttpEntity responseBody = response.getEntity();
                InputStream responseStream = responseBody.getContent();

                String responseString = IOUtils.toString(responseStream, StandardCharsets.UTF_8.name());
                response.close();
                responseStream.close();
                Logger.log("LoadBalancer | new backend port = " + responseString, "loadBalancerStartup");
                portInt = Integer.valueOf(responseString);
                capacityFactors.put(portInt, -1.0);
                Logger.log("LoadBalancer | backend ports:", "loadBalancerStartup");

                for (Map.Entry<Integer, Integer> entry : backendPortIndex.entrySet())
                    Logger.log(String.format("LoadBalancer | Index: %s | Port: %s", entry.getKey(), entry.getValue()), "loadBalancerStartup");

                break;
            } catch (UnsupportedEncodingException | UnsupportedOperationException | ClientProtocolException e) {
                System.out.println(e.toString() + " thrown in LoadBalancer#startupBackend");
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("InterruptedException thrown in LoadBalancer#startupBackend");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("IOException thrown in position 1 in LoadBalancer#startupBackend");
                e.printStackTrace();
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    System.out.println("IOException thrown in position 2 in LoadBalancer#startupBackend");
                    e.printStackTrace();
                }
            }
        }

        return portInt;
    }

    // PRIVATE HELPER METHODS
    private int selectPort(int resourceId) {
        int hashRingPointer = resourceId % HASH_RING_DENOMINATIONS;

        while (true) {
            if (backendPortIndex.containsKey(hashRingPointer))
                break;

            if (hashRingPointer == HASH_RING_DENOMINATIONS)
                hashRingPointer = 0;
            else
                hashRingPointer++;
        }

        return backendPortIndex.get(hashRingPointer);
    }

    // takes location of overloaded server as input and returns the location where a new server should be placed
    private int selectHashRingLocation(int backendPort) {
        Integer currLoc = null, prevLoc = null;

        List<Integer> locations = new ArrayList<>(backendPortIndex.keySet());
        List<Integer> ports = new ArrayList<>(backendPortIndex.values());
        Logger.log(String.format("LoadBalancer | locations = %s", locations), "capacityModulation");
        Logger.log(String.format("LoadBalancer | ports = %s", ports), "capacityModulation");

        if (ports.size() < 2) {
            int selectedPort = HASH_RING_DENOMINATIONS / 2;
            Logger.log(String.format("LoadBalancer | selectedPort = %d", selectedPort), "capacityModulation");
            return selectedPort;
        } else {
            if (ports.get(0) == backendPort) {
                // if backend is in the first pair in backendPortIndex
                currLoc = locations.get(0);
                prevLoc = locations.get(locations.size() - 1);
            } else {
                // otherwise
                for (int i = 0; i < ports.size(); i++) {
                    if (ports.get(i) == backendPort) {
                        prevLoc = currLoc;
                        currLoc = locations.get(i);
                        break;
                    } else {
                        currLoc = locations.get(i);
                    }
                }
            }

            Logger.log(String.format("LoadBalancer | currLoc = %d, prevLoc = %d", currLoc, prevLoc), "capacityModulation");
            int selectedPort = (currLoc + prevLoc) / 2;
            Logger.log(String.format("LoadBalancer | backendPort = %d, prevLoc = %d, currLoc = %d, selectedPort = %d", backendPort, prevLoc, currLoc, selectedPort), "capacityModulation");

            return selectedPort;
        }
    }
}
