package loadbalancer;

import loadbalancer.services.DemandFunction;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import loadbalancer.util.Logger;

public class Client implements Runnable {
    CloseableHttpClient httpClient;
    long maxDemandTime;
    private static int loadBalancerPort;
    int resourceId;
    String name;
    List<Integer> requestTimestamps;
    long requestStartTime;
    DemandFunction demandFunction;

    public Client(String _name, long maxDemandTime, DemandFunction demandFunction) {
        this.name = _name;
        Random random = new Random();
        this.maxDemandTime = maxDemandTime;
        this.resourceId = random.nextInt(10_000);
        this.requestTimestamps = Collections.synchronizedList(new ArrayList<>());
        // first request is sent up to 15 seconds after initialization to stagger the incoming requests
        this.requestStartTime = System.currentTimeMillis() + (long)((new Random()).nextInt(15000));
        this.demandFunction = demandFunction;
    }

    public static void setLoadBalancerPort(int port) {
        Client.loadBalancerPort = port;
    }

    @Override
    public void run() {
        Logger.log("Client | Started Client thread", "threadManagement");
        start();
        Logger.log("Client | Terminated Client thread", "threadManagement");
    }

    void start() {
        int count = 0;
        while (true) {
            if (System.currentTimeMillis() < this.requestStartTime) {
                // dummy printout used to force thread scheduling and thus even out client request load at beginning
//                Logger.log("", "alwaysPrint");
                count++;
                continue;
            }

            try {
                httpClient = HttpClients.createDefault();
                String path;
                path = "/api/" + resourceId;
                HttpGet httpget = new HttpGet("http://127.0.0.1:" + Client.loadBalancerPort + path);
                Logger.log(String.format("Client %s | path: %s", name, path), "clientStartup");
                long timestamp = System.currentTimeMillis();
                CloseableHttpResponse response = sendRequest(httpget);
                // record timestamp at which request was fired off to the load balancer
                this.requestTimestamps.add((int)(timestamp / 1000));
                printResponse(response);
                response.close();
                httpClient.close();
            } catch (IOException e) {
                System.out.println("Client | No response to request sent to load balancer by client server " + this.name);
            }

            try {
                this.demandFunction.rest();
            } catch (InterruptedException e) {
                Logger.log("Client | Client thread interrupted", "threadManagement");
                Thread.currentThread().interrupt();
                Logger.log("Client | Terminated Client Thread", "threadManagement");
                break;
            }
        }
    }

    // return a hash table mapping seconds since 1970 to number of requests sent
    public List<Integer> deliverData() {
        return this.requestTimestamps;
    }

    private CloseableHttpResponse sendRequest(HttpGet httpget) throws IOException {
        return httpClient.execute(httpget);
    }

    private void printResponse(CloseableHttpResponse response) throws IOException {
        HttpEntity responseBody = response.getEntity();
        InputStream bodyStream = responseBody.getContent();
        String responseString = IOUtils.toString(bodyStream, StandardCharsets.UTF_8.name());
        Logger.log(String.format("Client %s | response body: %s", name, responseString), "requestPassing");
        bodyStream.close();
    }
}
