package loadbalancerlab.client;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.services.DemandFunction;
import loadbalancerlab.shared.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Client implements Runnable {
    long maxDemandTime;
    private static int loadBalancerPort;
    int resourceId;
    String name;
    List<Integer> requestTimestamps;
    long requestStartTime;
    DemandFunction demandFunction;
    HttpClientFactory clientFactory;

    public Client( String _name, long maxDemandTime, DemandFunction demandFunction, HttpClientFactory clientFactory, long requestStartTime, int resourceId) {
        this.name = _name;
        Random random = new Random();
        this.maxDemandTime = maxDemandTime;
        this.resourceId = resourceId;
        this.requestTimestamps = Collections.synchronizedList(new ArrayList<>());
        // first request is sent up to 15 seconds after initialization to stagger the incoming requests
        this.requestStartTime = requestStartTime;
        this.demandFunction = demandFunction;
        this.clientFactory = clientFactory;
    }

    public static void setLoadBalancerPort(int port) {
        Client.loadBalancerPort = port;
    }

    @Override
    public void run() {
        Logger.log("Client | Started Client thread", Logger.LogType.THREAD_MANAGEMENT);
        start();
        Logger.log("Client | Terminated Client thread", Logger.LogType.THREAD_MANAGEMENT);
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
                String path;
                path = "/api/" + resourceId;
                HttpGet httpGet = new HttpGet("http://127.0.0.1:" + Client.loadBalancerPort + path);
                Logger.log(String.format("Client %s | path: %s", name, path), Logger.LogType.CLIENT_STARTUP);
                long timestamp = System.currentTimeMillis();
                CloseableHttpClient httpClient = this.clientFactory.buildApacheClient();
                CloseableHttpResponse response = httpClient.execute(httpGet);
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
                Logger.log("Client | Client thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
                Thread.currentThread().interrupt();
                Logger.log("Client | Terminated Client Thread", Logger.LogType.THREAD_MANAGEMENT);
                break;
            }
        }
    }

    // return a hash table mapping seconds since 1970 to number of requests sent
    public List<Integer> deliverData() {
        return this.requestTimestamps;
    }

    private void printResponse(CloseableHttpResponse response) throws IOException {
        HttpEntity responseBody = response.getEntity();
        InputStream bodyStream = responseBody.getContent();
        String responseString = IOUtils.toString(bodyStream, StandardCharsets.UTF_8.name());
        Logger.log(String.format("Client %s | response body: %s", name, responseString), Logger.LogType.REQUEST_PASSING);
        bodyStream.close();
    }
}
