package loadbalancer.monitor;

import loadbalancer.factory.ClientFactory;
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

    private ClientFactory clientFactory;
    private int backEndInitiatorPort;
    private ConcurrentMap<Integer, Double> capacityFactors;
    long initiationTime;
    private Map<Integer, Long> reinforcedTimes;
    private Map<Integer, Long> backEndStartTimes;
    private Map<Integer, Integer> backEndPortIndex;
    private RequestDecoder decoder;

    public CapacityFactorMonitorImpl(ClientFactory clientFactory, long initiationTime, int backEndInitiatorPort, RequestDecoder decoder) {
        this.clientFactory = clientFactory;
        this.reinforcedTimes = new ConcurrentHashMap<>();
        this.backEndStartTimes = new ConcurrentHashMap<>();
        this.capacityFactors = new ConcurrentHashMap<>();
        this.initiationTime = initiationTime;
        this.backEndInitiatorPort = backEndInitiatorPort;
        this.backEndPortIndex = new ConcurrentHashMap<>();
        this.decoder = decoder;
    }

    @Override
    public void pingServers(long currentTime) throws IOException {
        Logger.log("LoadBalancer - CapacityFactorMonitorImpl | CFMonitor loop running", "capacityModulation");
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();

        for (Map.Entry<Integer, Double> entry : capacityFactors.entrySet()) {
            int backEndPort = entry.getKey();
            Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | sending request for update on capacity factor to port %d", backEndPort), "telemetryUpdate");
            HttpGet httpGet = new HttpGet("http://127.0.0.1:" + backEndPort + "/capacity_factor");

            CloseableHttpResponse response = httpClient.execute(httpGet);
            JSONObject responseJson = this.decoder.extractJsonApacheResponse(response);

            double capacityFactor = responseJson.getDouble("capacity_factor");
            Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | received update on capacity factor: %s", capacityFactor), "telemetryUpdate");
            entry.setValue(capacityFactor);

            Logger.log("LoadBalancer - CapacityFactorMonitorImpl | cf = " + capacityFactor, "capacityModulation");

            if (currentTime > initiationTime + REST_INTERVAL) {
                if (capacityFactor > CAPACITY_FACTOR_MAX) {
                    if (reinforcedTimes.containsKey(backEndPort)) {
                        // if a server has been started up to reinforce this server recently
                        long lastReinforced = reinforcedTimes.get(backEndPort);
                        Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | last reinforced = %d", lastReinforced), "capacityModulation");

                        if (currentTime > lastReinforced + REINFORCEMENT_INTERVAL) {
                            // if the server was reinforced a while ago, clear it out from the list of recently reinforced servers
                            Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | clearing backendPort %d from reinforcedTimes", backEndPort), "capacityModulation");
                            reinforcedTimes.remove(backEndPort);
                            // startup a new dyno
                            reinforceServer(backEndPort, capacityFactor);
                        } else {
                            // if the server was reinforced recently, do not reinforce it again
                            Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | skipping reinforcement of port %d", backEndPort), "capacityModulation");
                        }
                    } else {
                        // if the server has not been reinforced recently
                        reinforceServer(backEndPort, capacityFactor);
                    }
                } else if (capacityFactor < CAPACITY_FACTOR_MIN && currentTime > this.backEndStartTimes.get(backEndPort) + MIN_TIME_TO_LIVE) {
                    // if the server is underutilized and it has been running for at least MIN_TIME_TO_LIVE
                    // shutdown server
                    Logger.log(String.format("LoadBalancer | backendPort %d is underutilized with cf = %f", backEndPort, capacityFactor), "capacityModulation");
                    shutDownBackEnd(backEndPort);
                }
            }
        }

        httpClient.close();
    }

    public int selectPort(int resourceId) {
        int hashRingPointer = resourceId % HASH_RING_DENOMINATIONS;

        while (true) {
            // rotate clockwise on the hash ring until a backend port is found
            if (this.backEndPortIndex.containsKey(hashRingPointer))
                break;

            if (hashRingPointer == HASH_RING_DENOMINATIONS)
                hashRingPointer = 0;
            else
                hashRingPointer++;
        }

        return this.backEndPortIndex.get(hashRingPointer);
    }

    public int startUpBackEnd(int hashRingIndex) {
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
        HttpPost httpPost = new HttpPost("http://127.0.0.1:" + this.backEndInitiatorPort + "/backends");

        int portInt = -1;

        while(true) {
            try {
                Thread.sleep(100);
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | sent request to startup a backend", "capacityModulation");
                CloseableHttpResponse response = httpClient.execute(httpPost);
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | received response", "capacityModulation");
                JSONObject jsonObj = this.decoder.extractJsonApacheResponse(response);
                portInt = jsonObj.getInt("port");
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | new backend port = " + portInt, "capacityModulation");
                capacityFactors.put(portInt, -1.0);
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | backend ports:", "loadBalancerStartup");

                for (Map.Entry<Integer, Integer> entry : this.backEndPortIndex.entrySet())
                    Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | Index: %s | Port: %s", entry.getKey(), entry.getValue()), "loadBalancerStartup");

            } catch (UnsupportedEncodingException | UnsupportedOperationException | ClientProtocolException e) {
                System.out.println(e.toString() + " thrown in LoadBalancer#startupBackend");
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("InterruptedException thrown in LoadBalancer#startupBackend");
            } catch (IOException e) {
                System.out.println("IOException thrown in position 1 in LoadBalancer#startupBackend");
                e.printStackTrace();
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    System.out.println("IOException thrown in position 2 in LoadBalancer#startupBackend");
                }
                break;
            }
        }

        this.backEndPortIndex.put(hashRingIndex, portInt);
        this.backEndStartTimes.put(portInt, System.currentTimeMillis());

        return portInt;
    }

    @Override
    public void shutDownBackEnd(int backEndPort) {
        CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
        HttpDelete httpDelete = new HttpDelete("http://127.0.0.1:" + this.backEndInitiatorPort + "/backend/" + backEndPort);

        try {
            Thread.sleep(100);
            Logger.log("LoadBalancer | sent request to shutdown backend running on port " + backEndPort, "capacityModulation");
            httpClient.execute(httpDelete);
        } catch (InterruptedException e) {
            System.out.println("InterruptedException thrown in LoadBalancer#shutdownBackend");
        } catch (IOException e) {
            System.out.println("IOException thrown in position 1 LoadBalancer#shutdownBackend");
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                System.out.println("IOException thrown in position 2 in LoadBalancer#shutdownBackend");
            }
            capacityFactors.remove(backEndPort);
        }
    }

    private void reinforceServer(int backEndPort, double capacityFactor) {
        // startup a new dyno
        Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | backendPort %d is overloaded with cf = %f", backEndPort, capacityFactor), "capacityModulation");
        int newServerHashRingLocation = selectHashRingLocation(backEndPort);
        if (newServerHashRingLocation == -1) {
            return;
        }

        Logger.log(String.format("LoadBalancer - CapacityFactorMonitorImpl | selected location %d for new server", newServerHashRingLocation), "capacityModulation");
        // start a new server at the new hash ring location
        startUpBackEnd(newServerHashRingLocation);
        // record that backendPort was reinforced
        reinforcedTimes.put(backEndPort, System.currentTimeMillis());
    }

    // takes location of overloaded server as input and returns the location where a new server should be placed
    // returns -1 if there's no place to put the server on the hash ring
    private int selectHashRingLocation(int backendPort) {
        Integer currLoc = null, prevLoc = null;

        List<Integer> locations = new ArrayList<>(this.backEndPortIndex.keySet());
        Collections.sort(locations);
        int selectedLocation;

        if (locations.size() == 0) {
            selectedLocation = 0;
        } else if (locations.size() == 1) {
            selectedLocation = HASH_RING_DENOMINATIONS / 2;
        } else {
            int firstLocation = locations.get(0);
            if (this.backEndPortIndex.get(firstLocation) == backendPort) {
                // if backend is in the first position in hash ring
                currLoc = locations.get(0);
                // then the previous server is the server in the last position
                prevLoc = locations.get(locations.size() - 1);
            } else {
                // otherwise
                // the previous server is the server in the next position proceeding counterclockwise from the server
                for (int i = 0; i < locations.size(); i++) {
                    if (this.backEndPortIndex.get(locations.get(i)) == backendPort) {
                        prevLoc = currLoc;
                        currLoc = locations.get(i);
                        break;
                    } else {
                        currLoc = locations.get(i);
                    }
                }

                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | backendPort = " + backendPort, "capacityModulation");
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | backendPortIndex:", "capacityModulation");

                for (Map.Entry<Integer, Integer> entry : this.backEndPortIndex.entrySet()) {
                    Logger.log("entry: " + entry.getKey() + " | " + entry.getValue(), "capacityModulation");
                }

                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | selectHashRingLocation | currLoc = " + currLoc, "capacityModulation");
                Logger.log("LoadBalancer - CapacityFactorMonitorImpl | selectHashRingLocation | prevLoc = " + prevLoc, "capacityModulation");

            }
            selectedLocation = (currLoc + prevLoc) / 2;
        }

        if (this.backEndPortIndex.containsKey(selectedLocation)) {
            return -1;
        } else {
            return selectedLocation;
        }
    }
}