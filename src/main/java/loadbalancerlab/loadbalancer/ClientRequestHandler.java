package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ClientRequestHandler implements HttpRequestHandler {
    private List<Integer> incomingRequestTimestamps;
    private CacheRedistributor cacheRedis;
    private static HttpClientFactory clientFactory;

    public static void configure( Config config ) {
        clientFactory = config.getClientFactory();
    }

    public ClientRequestHandler(CacheRedistributor _cacheRedis) {
        incomingRequestTimestamps = Collections.synchronizedList(new LinkedList<>());
        cacheRedis = _cacheRedis;
    }

    @Override
    public void handle( HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) {
        System.out.println("handle firing");
        CloseableHttpClient httpClient = clientFactory.buildApacheClient();
        System.out.println("path a");
        String uri = httpRequest.getRequestLine().getUri();
        String[] uriArr = uri.split("/", 0);
        System.out.println("path b");
        int cacheServerPort = cacheRedis.selectPort("Chooder_Bunny");
        System.out.println("path c");

        Logger.log(String.format("ClientRequestHandler | relaying message to cache server at port %d", cacheServerPort), Logger.LogType.REQUEST_PASSING);

        // record request incoming timestamp
        incomingRequestTimestamps.add((int)(System.currentTimeMillis() / 1000));
        System.out.println("path d");

//        try {
//            httpResponse.setEntity(new StringEntity("test"));
//        } catch (UnsupportedEncodingException e) {
//            System.out.println("encoding not supported");
//            e.printStackTrace();
//        }

        System.out.println("path e");
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + cacheServerPort);

        try {
            CloseableHttpResponse res = httpClient.execute(getReq);
        } catch (IOException e) {

        }

//        try {
//            CloseableHttpResponse response = httpClient.execute();
//            Logger.log("ClientRequestHandler | sent message", Logger.LogType.REQUEST_PASSING);
//            HttpEntity responseBody = response.getEntity();
//            httpResponse.setEntity(responseBody);
//        } catch (IOException e) {
//            // if request to cache server failed
//            JSONObject outputJsonObj = new JSONObject();
//            outputJsonObj.put("error_message", "Cache server failed to respond");
//            String htmlResponse = StringEscapeUtils.escapeJson(outputJsonObj.toString());
//            InputStream stream = new ByteArrayInputStream(htmlResponse.getBytes());
//            BasicHttpEntity responseBody = new BasicHttpEntity();
//            responseBody.setContent(stream);
//            httpResponse.setStatusCode(500);
//            httpResponse.setEntity((HttpEntity)responseBody);
//            System.out.println("ClientRequestHandler | IOException : Cache server failed to respond.");
//        }
    }
}