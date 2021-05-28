package loadbalancer.services.monitor;

import loadbalancer.factory.HttpClientFactory;
import loadbalancer.util.RequestDecoder;
import loadbalancer.util.Logger;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CapacityFactorMonitorImpl implements CapacityFactorMonitor {
    private final double CAPACITY_FACTOR_MAX = 0.75;
    private final double CAPACITY_FACTOR_MIN = 0.25;
    private final int REST_INTERVAL = 5_000;
    private final int REINFORCEMENT_INTERVAL = 5_000;
    private final int MIN_TIME_TO_LIVE = 5_000;
    private final int HASH_RING_DENOMINATIONS = 6_000;

    private HttpClientFactory clientFactory;
    private int cacheServerManagerPort;
    private ConcurrentMap<Integer, Double> capacityFactors;
    long initiationTime;
    private Map<Integer, Long> reinforcedTimes;
    private Map<Integer, Long> cacheServerStartTimes;
    private Map<Integer, Integer> cacheServerPortIndex;
    private RequestDecoder decoder;

    public CapacityFactorMonitorImpl(HttpClientFactory clientFactory, long initiationTime, int cacheServerManagerPort, RequestDecoder decoder) {
        this.clientFactory = clientFactory;
        this.reinforcedTimes = new ConcurrentHashMap<>();
        this.cacheServerStartTimes = new ConcurrentHashMap<>();
        this.capacityFactors = new ConcurrentHashMap<>();
        this.initiationTime = initiationTime;
        this.cacheServerManagerPort = cacheServerManagerPort;
        this.cacheServerPortIndex = new ConcurrentHashMap<>();
        this.decoder = decoder;
    }

    @Override
    public void pingServers(long currentTime) throws IOException {
        Logger.log("LoadBalancer - CapacityFactorMonitorImpl | CFMonitor loop running", Logger.LogType.CAPACITY_MODULATION);
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();

        for (Map.Entry<Integer, Double> entry : capacityFactors.entrySet()) {
            int cacheServerPort = entry.getKey();
            Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | sending request for update on capacity factor to port %d", cacheServerPort), Logger.LogType.TELEMETRY_UPDATE);
            HttpGet httpGet = new HttpGet("http://127.0.0.1:" + cacheServerPort + "/capacity_factor");

            CloseableHttpResponse response = httpClient.execute(httpGet);
            JSONObject responseJson = this.decoder.extractJsonApacheResponse(response);

            double capacityFactor = responseJson.getDouble("capacity_factor");
            Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | received update on capacity factor: %s", capacityFactor), Logger.LogType.TELEMETRY_UPDATE);
            entry.setValue(capacityFactor);

            Logger.log("LoadBalancer - CapacityFactorMonitorImpl | cf = " + capacityFactor, Logger.LogType.CAPACITY_MODULATION);

            if (currentTime > initiationTime + REST_INTERVAL) {
                if (capacityFactor > CAPACITY_FACTOR_MAX) {
                    if (reinforcedTimes.containsKey(cacheServerPort)) {
                        // if a server has been started up to reinforce this server recently
                        long lastReinforced = reinforcedTimes.get(cacheServerPort);
                        Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | last reinforced = %d", lastReinforced), Logger.LogType.CAPACITY_MODULATION);

                        if (currentTime > lastReinforced + REINFORCEMENT_INTERVAL) {
                            // if the server was reinforced a while ago, clear it out from the list of recently reinforced servers
                            Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | clearing cache server port %d from reinforcedTimes", cacheServerPort), Logger.LogType.CAPACITY_MODULATION);
                            reinforcedTimes.remove(cacheServerPort);
                            // startup a new dyno
                            reinforceServer(cacheServerPort, capacityFactor);
                        } else {
                            // if the server was reinforced recently, do not reinforce it again
                            Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | skipping reinforcement of port %d", cacheServerPort), Logger.LogType.CAPACITY_MODULATION);
                        }
                    } else {
                        // if the server has not been reinforced recently
                        reinforceServer(cacheServerPort, capacityFactor);
                    }
                } else if (capacityFactor < CAPACITY_FACTOR_MIN && currentTime > this.cacheServerStartTimes.get(cacheServerPort) + MIN_TIME_TO_LIVE) {
                    // if the server is underutilized and it has been running for at least MIN_TIME_TO_LIVE
                    // shutdown server
                    Logger.log(String.format("LoadBalancer | cache server port %d is underutilized with cf = %f", cacheServerPort, capacityFactor), Logger.LogType.CAPACITY_MODULATION);
                    shutdownCacheServer(cacheServerPort);
                }
            }
        }

        httpClient.close();
    }

    public int selectPort(int resourceId) {
        int hashRingPointer = resourceId % HASH_RING_DENOMINATIONS;

        while (true) {
            // rotate clockwise on the hash ring until a cache server port is found
            if (this.cacheServerPortIndex.containsKey(hashRingPointer))
                break;

            if (hashRingPointer == HASH_RING_DENOMINATIONS)
                hashRingPointer = 0;
            else
                hashRingPointer++;
        }

        return this.cacheServerPortIndex.get(hashRingPointer);
    }

    public int startupCacheServer(int hashRingIndex) {
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
        HttpPost httpPost = new HttpPost("http://127.0.0.1:" + this.cacheServerManagerPort + "/cache-servers");

        int portInt = -1;

        while(true) {
            try {
                Thread.sleep(100);
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | sent request to startup a cache server", Logger.LogType.CAPACITY_MODULATION);
                CloseableHttpResponse response = httpClient.execute(httpPost);
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | received response", Logger.LogType.CAPACITY_MODULATION);
                JSONObject jsonObj = this.decoder.extractJsonApacheResponse(response);
                portInt = jsonObj.getInt("port");
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | new cache server port = " + portInt, Logger.LogType.CAPACITY_MODULATION);
                capacityFactors.put(portInt, -1.0);
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | cache server ports:", Logger.LogType.LOAD_BALANCER_STARTUP);

                for (Map.Entry<Integer, Integer> entry : this.cacheServerPortIndex.entrySet())
                    Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | Index: %s | Port: %s", entry.getKey(), entry.getValue()), Logger.LogType.LOAD_BALANCER_STARTUP);

            } catch (UnsupportedEncodingException | UnsupportedOperationException | ClientProtocolException e) {
                System.out.println(e.toString() + " thrown in LoadBalancer#startupCacheServer");
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("InterruptedException thrown in LoadBalancer#startupCacheServer");
            } catch (IOException e) {
                System.out.println("IOException thrown in position 1 in LoadBalancer#startupCacheServer");
                e.printStackTrace();
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    System.out.println("IOException thrown in position 2 in LoadBalancer#startupCacheServer");
                }
                break;
            }
        }

        this.cacheServerPortIndex.put(hashRingIndex, portInt);
        this.cacheServerStartTimes.put(portInt, System.currentTimeMillis());

        return portInt;
    }

    @Override
    public void shutdownCacheServer(int cacheServerPort) {
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
        HttpDelete httpDelete = new HttpDelete("http://127.0.0.1:" + this.cacheServerManagerPort + "/cache-server/" + cacheServerPort);

        try {
            Thread.sleep(100);
            Logger.log("LoadBalancer | sent request to shutdown cache server running on port " + cacheServerPort, Logger.LogType.CAPACITY_MODULATION);
            httpClient.execute(httpDelete);
        } catch (InterruptedException e) {
            System.out.println("InterruptedException thrown in LoadBalancer#shutdownCacheServer");
        } catch (IOException e) {
            System.out.println("IOException thrown in position 1 LoadBalancer#shutdownCacheServer");
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                System.out.println("IOException thrown in position 2 in LoadBalancer#shutdownCacheServer");
            }
            capacityFactors.remove(cacheServerPort);
        }
    }

    private void reinforceServer(int cacheServerPort, double capacityFactor) {
        // startup a new dyno
        Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | cache server port %d is overloaded with cf = %f", cacheServerPort, capacityFactor), Logger.LogType.CAPACITY_MODULATION);
        int newServerHashRingLocation = selectHashRingLocation(cacheServerPort);
        if (newServerHashRingLocation == -1) {
            return;
        }

        Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | selected location %d for new server", newServerHashRingLocation), Logger.LogType.CAPACITY_MODULATION);
        // start a new server at the new hash ring location
        startupCacheServer(newServerHashRingLocation);
        // record that cache server port was reinforced
        reinforcedTimes.put(cacheServerPort, System.currentTimeMillis());
    }

    // takes location of overloaded server as input and returns the location where a new server should be placed
    // returns -1 if there's no place to put the server on the hash ring
    private int selectHashRingLocation(int cacheServerPort) {
        Integer currLoc = null, prevLoc = null;

        List<Integer> locations = new ArrayList<>(this.cacheServerPortIndex.keySet());
        Collections.sort(locations);
        int selectedLocation;

        if (locations.size() == 0) {
            selectedLocation = 0;
        } else if (locations.size() == 1) {
            selectedLocation = HASH_RING_DENOMINATIONS / 2;
        } else {
            int firstLocation = locations.get(0);
            if (this.cacheServerPortIndex.get(firstLocation) == cacheServerPort) {
                // if cache server is in the first position in hash ring
                currLoc = locations.get(0);
                // then the previous server is the server in the last position
                prevLoc = locations.get(locations.size() - 1);
            } else {
                // otherwise
                // the previous server is the server in the next position proceeding counterclockwise from the server
                for (int i = 0; i < locations.size(); i++) {
                    if (this.cacheServerPortIndex.get(locations.get(i)) == cacheServerPort) {
                        prevLoc = currLoc;
                        currLoc = locations.get(i);
                        break;
                    } else {
                        currLoc = locations.get(i);
                    }
                }

                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | cache server = " + cacheServerPort, Logger.LogType.CAPACITY_MODULATION);
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | cacheServerPortIndex:", Logger.LogType.CAPACITY_MODULATION);

                for (Map.Entry<Integer, Integer> entry : this.cacheServerPortIndex.entrySet()) {
                    Logger.log("entry: " + entry.getKey() + " | " + entry.getValue(), Logger.LogType.CAPACITY_MODULATION);
                }

                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | selectHashRingLocation | currLoc = " + currLoc, Logger.LogType.CAPACITY_MODULATION);
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | selectHashRingLocation | prevLoc = " + prevLoc, Logger.LogType.CAPACITY_MODULATION);

            }
            selectedLocation = (currLoc + prevLoc) / 2;
        }

        if (this.cacheServerPortIndex.containsKey(selectedLocation)) {
            return -1;
        } else {
            return selectedLocation;
        }
    }
}