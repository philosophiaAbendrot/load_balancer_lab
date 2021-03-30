package loadbalancer.util;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.JSONObject;

import java.io.IOException;

public interface Decoder {
    // extract JSON object from an Apache CloseableHttpResponse
    JSONObject extractJsonApacheResponse( CloseableHttpResponse response) throws IOException;
}
