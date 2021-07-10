package loadbalancerlab.shared;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Used for extracting JSON parameters from a CloseableHttpResponse object
 */
public class RequestDecoder {

    /**
     * @param response      org.apache.http.CloseableHttpResponse object
     * @return              Returns JSONObject which is extracted from the response
     * @throws IOException  If an IOException occurs
     */
    public JSONObject extractJsonApacheResponse(CloseableHttpResponse response) throws IOException {
        HttpEntity responseBody = response.getEntity();
        InputStream responseStream = null;
        String responseString = null;
        JSONObject jsonObject = null;

        responseStream = responseBody.getContent();     /* Throws IOException if stream could not be created */
        responseString = IOUtils.toString(responseStream, StandardCharsets.UTF_8.name());
        jsonObject = new JSONObject(StringEscapeUtils.unescapeJson(responseString));

        responseStream.close();     /* Throws IOException if error occurs with closing stream */
        response.close();           /* Throws IOException if error occurs with closign CloseableHttpResponse object */

        return jsonObject;
    }
}