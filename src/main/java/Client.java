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
import java.util.concurrent.TimeUnit;

public class Client implements Runnable {
    CloseableHttpClient httpClient;
    long maxDemandTime;
    private static final int LOAD_BALANCER_PORT = 8080;
    int resourceId;
    String name;
    List<Integer> requestTimestamps;
    long requestStartTime;

    public Client(String _name, long maxDemandTime) {
        this.name = _name;
        Random random = new Random();
        this.maxDemandTime = maxDemandTime;
        this.resourceId = random.nextInt(3000);
        this.requestTimestamps = Collections.synchronizedList(new ArrayList<>());
        // first request is sent up to 15 seconds after initialization to stagger the incoming requests
        this.requestStartTime = System.currentTimeMillis() + (long)((new Random()).nextInt(15000));
    }

    @Override
    public void run() {
        Logger.log("Client | Started Client thread", "threadManagement");

        try {
            start();
        } catch(IOException e) {
            System.out.println("IOException thrown in Client#run");
            e.printStackTrace();
        }

        Logger.log("Client | Terminated Client thread", "threadManagement");
    }

    void start() throws IOException {
        int count = 0;
        while (true) {
            if (System.currentTimeMillis() < this.requestStartTime) {
                // dummy printout used to force thread scheduling and thus even out client request load at beginning
                Logger.log("", "alwaysPrint");
                count++;
                continue;
            }

            httpClient = HttpClients.createDefault();
            String path;
            path = "/api/" + resourceId;
            HttpGet httpget = new HttpGet("http://127.0.0.1:" + LOAD_BALANCER_PORT + path);
            Logger.log(String.format("Client %s | path: %s", name, path), "clientStartup");
            long timestamp = System.currentTimeMillis();
            CloseableHttpResponse response = sendRequest(httpget);
            // record timestamp at which request was fired off to the load balancer
            this.requestTimestamps.add((int)(timestamp / 1000));
            printResponse(response);
            response.close();
            httpClient.close();

            try {
                int freq = requestFrequency();
                Logger.log("Client | frequency = " + freq, "loadModulation");
                TimeUnit.MILLISECONDS.sleep(freq);
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

    private int requestFrequency() {
        long x = System.currentTimeMillis();
        double variabilityRange = 0.6;

        // demand function
        // -0.0005(x - 20)^2 + 0.5

        if (Math.abs(x - maxDemandTime) >= 19500) {
            return Integer.MAX_VALUE;
        } else {
            // demand curve is a downward facing parabola
            double delta = (x - maxDemandTime) / 1000.0;
            double demand = Math.max(-0.0005 * Math.pow(delta, 2) + 0.5, 0.07);
            // introduce variability
            Random rand = new Random();
            double variabilityFactor = 0.7 +  0.6 * rand.nextDouble();
            Logger.log("Client | demand = " + demand, "recordingData");
            int waitTime = (int)Math.round(1000 / demand * variabilityFactor);
            Logger.log("Client | waitTime = " + waitTime, "recordingData");
            return waitTime;
        }
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
