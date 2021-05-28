package loadbalancerlab.util;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.JSONObject;

import java.io.IOException;

public interface RequestDecoder {
    // extract JSON object from an Apache CloseableHttpResponse
    // closes response
    JSONObject extractJsonApacheResponse( CloseableHttpResponse response) throws IOException;
}
