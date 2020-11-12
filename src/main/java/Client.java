import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpClientConnectionFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


public class Client {
    public static void sendGetRequest() throws IOException, InterruptedException, HttpException {
//        InetAddress host = InetAddress.getLocalHost();
//        int port = 8080;
//        Socket socket = new Socket("127.0.0.1", port);
//        System.out.println("opened socket on port " + port);
//        DefaultBHttpClientConnection conn = new DefaultBHttpClientConnection(1024);
//        conn.bind(socket);
//        HttpRequest request = new BasicHttpRequest("GET", "/");
//        System.out.println("sending request header");
//        conn.sendRequestHeader(request);
//        System.out.println("waiting for response");
//        HttpResponse response = conn.receiveResponseHeader();
//        conn.receiveResponseEntity(response);
//        System.out.println("received response");
//        HttpEntity entity = response.getEntity();
//        if (entity != null) {
//            EntityUtils.consume(entity);
//        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://127.0.0.1:8080");
        System.out.println("started");
        CloseableHttpResponse response = httpClient.execute(httpget);
        System.out.println("sent request");
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
