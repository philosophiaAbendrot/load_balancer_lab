import org.apache.http.*;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpClientConnectionFactory;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


public class Client {
    public static void sendGetRequest() throws IOException, InterruptedException, HttpException {
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("http://localhost:6666"))
//                .build();
//
//        HttpResponse<String> response = client.send(request,
//                HttpResponse.BodyHandlers.ofString());
//        System.out.println(response.body());
        InetAddress host = InetAddress.getLocalHost();
        Socket socket = new Socket(host, 8080);
        System.out.println("opened socket on port 6666");
        DefaultBHttpClientConnection conn = new DefaultBHttpClientConnection(1024);
        conn.bind(socket);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        System.out.println("sending request header");
        conn.sendRequestHeader(request);
        System.out.println("waiting for response");
        HttpResponse response = conn.receiveResponseHeader();
        conn.receiveResponseEntity(response);
        System.out.println("received response");
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            EntityUtils.consume(entity);
        }
    }

    public static void main(String[] args) {
        try {
            sendGetRequest();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } catch (HttpException e) {
            System.out.println(e.getMessage());
        }
    }
}
