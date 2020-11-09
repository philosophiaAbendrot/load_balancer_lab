import org.apache.http.*;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;


import java.net.*;
import java.io.*;

public class Server {
    public void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String inputLine;

            SessionInputBufferImpl inBuffer = new SessionInputBufferImpl(
                    new HttpTransportMetricsImpl(), 256);
            inBuffer.bind(clientSocket.getInputStream());

            HttpMessageParser<HttpRequest> requestParser = new DefaultHttpRequestParser(inBuffer);
            HttpRequest request = requestParser.parse();
            HeaderIterator iterator = request.headerIterator();

            System.out.println("request headers:");

            while (iterator.hasNext()) {
                Header header = iterator.nextHeader();
                System.out.printf("%s: %s\n", header.getName(), header.getValue());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (HttpException e) {
            System.out.println(e.getMessage());
        }
    }

    public void generateResponse() {
        ProtocolVersion http = new ProtocolVersion("HTTP", 1, 1);
        HttpContext context = new HttpCoreContext();
        DefaultHttpResponseFactory responseFactory = new DefaultHttpResponseFactory();
        HttpResponse resp = responseFactory.newHttpResponse(http, 200, context);
    }

    public static void main(String[] args) {
        new Server().start(6666);
    }
}
