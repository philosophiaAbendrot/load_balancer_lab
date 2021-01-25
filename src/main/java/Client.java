import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable {
    CloseableHttpClient httpClient;
    private static final int LOAD_BALANCER_PORT = 8080;
    private static final int NUM_REQUESTS = 3;
    int resourceId;
    String name;

    public Client(String _name) {
//        httpClient = HttpClients.createDefault();
        name = _name;
        Random random = new Random();
        resourceId = random.nextInt(3000);
    }

    @Override
    public void run() {
        Logger.log("Client | Started Client thread", "threadManagement");

        try {
            start();
        } catch(IOException e) {
            System.out.println("Client did not receive a response.");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        Logger.log("Client | Terminated Client thread", "threadManagement");
    }

    void start() throws IOException {
        while (true) {
            httpClient = HttpClients.createDefault();
            String path;
            path = "/api/" + resourceId;
            HttpGet httpget = new HttpGet("http://127.0.0.1:" + LOAD_BALANCER_PORT + path);
            Logger.log(String.format("Client %s | path: %s", name, path), "clientStartup");
            CloseableHttpResponse response = sendRequest(httpget);

            printResponse(response);
            response.close();
            httpClient.close();

            try {
                TimeUnit.MILLISECONDS.sleep(1500);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                Thread.currentThread().interrupt();
                break;
            }
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
