import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;

public class Client {
    public static void sendGetRequest() throws IOException, InterruptedException, HttpException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://127.0.0.1:8080");
        System.out.println("started");
        CloseableHttpResponse response = httpClient.execute(httpget);
        System.out.println("sent request");
        System.out.println("received response");
        StatusLine statusLine = response.getStatusLine();
        System.out.println("status = " + statusLine.getStatusCode());
        Header[] headers = response.getAllHeaders();
        System.out.println("header length = " + headers.length);

        for (int i = 0; i < headers.length; i++) {
            Header header = headers[i];
            System.out.printf("header: %s: %s\n", header.getName(), header.getValue());
        }

        response.close();
    }

    public static void main(String[] args) {
        try {
            sendGetRequest();
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
