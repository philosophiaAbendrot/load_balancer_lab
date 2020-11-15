import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable {
    CloseableHttpClient httpClient;
    private static final int LOAD_BALANCER_PORT = 8080;

    public Client() {
        httpClient = HttpClients.createDefault();
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
        HttpGet httpget = new HttpGet("http://127.0.0.1:" + LOAD_BALANCER_PORT + "/order");

        for (int i = 0; i < 5; i++) {
            System.out.println("Client | client sent request");
            CloseableHttpResponse response = sendRequest(httpget);
            printResponse(response);

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

        System.out.println("Client | response headers:");
        for (int i = 0; i < headers.length; i++) {
            Header header = headers[i];
            System.out.printf("Client | header: %s: %s\n", header.getName(), header.getValue());
        }

        System.out.println("Client | response body:");
        HttpEntity responseBody = response.getEntity();
        InputStream bodyStream = responseBody.getContent();
        String responseString = IOUtils.toString(bodyStream, StandardCharsets.UTF_8.name());
        System.out.println(responseString);
        bodyStream.close();
        response.close();
    }
}
