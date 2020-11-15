import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Client implements Runnable {
    public void run() {
        try {
            start();
        } catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (HttpException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    void start() throws IOException, InterruptedException, HttpException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://127.0.0.1:8080");
        System.out.println("client sent request");
        CloseableHttpResponse response = httpClient.execute(httpget);
        StatusLine statusLine = response.getStatusLine();
        Header[] headers = response.getAllHeaders();

        System.out.println("response headers:");
        for (int i = 0; i < headers.length; i++) {
            Header header = headers[i];
            System.out.printf("header: %s: %s\n", header.getName(), header.getValue());
        }

        System.out.println("response body:");
        HttpEntity responseBody = response.getEntity();
        InputStream bodyStream = responseBody.getContent();
        String responseString = IOUtils.toString(bodyStream, StandardCharsets.UTF_8.name());
        System.out.println(responseString);
        bodyStream.close();
        response.close();
    }

    public static void main(String[] args) {
        try {
            new Client().start();
        } catch (IOException e) {
            System.out.println("IOException");
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("InterruptedException");
            System.out.println(e.getMessage());
        } catch (HttpException e) {
            System.out.println("HTTPException");
            System.out.println(e.getMessage());
        }
    }
}
