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
    String name;

    public Client(String _name) {
        httpClient = HttpClients.createDefault();
        name = _name;
    }

    @Override
    public void run() {
        try {
            start();
        } catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    void start() throws IOException {
        Random rand = new Random();

        for (int i = 0; i < 5; i++) {
            int roll = rand.nextInt(2);
            String path;
            if (roll == 1) {
                path = "/home";
            } else {
                path = "/image";
            }
            HttpGet httpget = new HttpGet("http://127.0.0.1:" + LOAD_BALANCER_PORT + path);
            Logger.log(String.format("Client %s | path: %s", name, path));
            CloseableHttpResponse response = sendRequest(httpget);
            printResponse(response);
            response.close();

            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private CloseableHttpResponse sendRequest(HttpGet httpget) throws IOException {
        return httpClient.execute(httpget);
    }

    private void printResponse(CloseableHttpResponse response) throws IOException {
        StatusLine statusLine = response.getStatusLine();
        Header[] headers = response.getAllHeaders();

//        System.out.printf("Client %s | response headers:\n", name);
//        for (int i = 0; i < headers.length; i++) {
//            Header header = headers[i];
//            System.out.printf("Client | header: %s: %s\n", header.getName(), header.getValue());
//        }

        HttpEntity responseBody = response.getEntity();
        InputStream bodyStream = responseBody.getContent();
        String responseString = IOUtils.toString(bodyStream, StandardCharsets.UTF_8.name());
        Logger.log(String.format("Client %s | response body: %s", name, responseString));
        bodyStream.close();
    }
}
