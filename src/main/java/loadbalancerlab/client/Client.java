package loadbalancerlab.client;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Client implements Runnable {
    /**
     * Timestamp at which demand peaks. Used for certain demand functions. Milliseconds since 1-Jan-1970
     */
    long maxDemandTime;
    /**
     * The port on which the load balancer is running
     */
    private static int loadBalancerPort;
    /**
     * Name of the resource that is being fetched from the service
     */
    String resourceName;

    /**
     * Timestamp at which client starts sending requests. Milliseconds since 1-Jan-1970.
     */
    long requestStartTime;

    /**
     * Associated DemandFunction. This function regulates the time between requests, and thus the total request load to the load balancer.
     */
    DemandFunction demandFunction;

    /**
     * A factory which generates CloseableHttpClient instances which send off http requests.
     */
    HttpClientFactory clientFactory;

    public Client( long maxDemandTime, DemandFunction demandFunction, HttpClientFactory clientFactory, long requestStartTime ) {
        this.maxDemandTime = maxDemandTime;
        // first request is sent up to 15 seconds after initialization to stagger the incoming requests
        this.requestStartTime = requestStartTime;
        this.demandFunction = demandFunction;
        this.clientFactory = clientFactory;
    }

    /**
     * Sets the load balancer port field so the Client instances know where to send requests.
     * @param port: The port that the load balancer is running on
     */
    public static void setLoadBalancerPort(int port) {
        Client.loadBalancerPort = port;
    }

    /**
     * Method for Runnable interface.
     * Starts client instance. Makes the client instance send a request to the load balancer, amd then rests an interval
     * regulated by the DemandFunction, and repeats.
     */
    @Override
    public void run() {
        Logger.log("Client | Started Client thread", Logger.LogType.THREAD_MANAGEMENT);
        int count = 0;
        while (true) {
            if (System.currentTimeMillis() < requestStartTime) {
                // dummy printout used to force thread scheduling and thus even out client request load at beginning
//                Logger.log("", "alwaysPrint");
                count++;
                continue;
            }

            try {
                resourceName = RandomStringUtils.randomAlphabetic(10);
                CloseableHttpResponse res = sendResponse(resourceName);
                printResponse(res);
                res.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Client | No response to request sent to load balancer by client server ");
            }

            try {
                this.demandFunction.rest();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.log("Client | Client thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
                Thread.currentThread().interrupt();
                Logger.log("Client | Terminated Client Thread", Logger.LogType.THREAD_MANAGEMENT);
                break;
            }
        }
        Logger.log("Client | Terminated Client thread", Logger.LogType.THREAD_MANAGEMENT);
    }

    public CloseableHttpResponse sendResponse(String resourceName) throws IOException {
        String path = "/api/" + resourceName;
        HttpGet httpGet = new HttpGet("http://127.0.0.1:" + Client.loadBalancerPort + path);
        System.out.println("Client | sending request to loadbalancer at path = " + httpGet.getURI().toString());
        Logger.log(String.format("Client | path: %s", path), Logger.LogType.CLIENT_STARTUP);
        CloseableHttpClient httpClient = clientFactory.buildApacheClient();
        CloseableHttpResponse res = httpClient.execute(httpGet);
        httpClient.close();
        return res;
    }

    /**
     * Helper method for printing responses from the load balancer
     * @param response: the response object passed from the load balancer
     * @throws IOException: Throws exception if there is a failure in IO operations
     */
    private void printResponse(CloseableHttpResponse response) throws IOException {
        HttpEntity responseBody = response.getEntity();
        InputStream bodyStream = responseBody.getContent();
        String responseString = IOUtils.toString(bodyStream, StandardCharsets.UTF_8.name());
        Logger.log(String.format("Client | response body: %s", responseString), Logger.LogType.REQUEST_PASSING);
        bodyStream.close();
    }
}
