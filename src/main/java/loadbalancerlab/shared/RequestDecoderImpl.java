package loadbalancerlab.shared;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class RequestDecoderImpl implements RequestDecoder {
    @Override
    public JSONObject extractJsonApacheResponse(CloseableHttpResponse response) throws IOException {
        HttpEntity responseBody = response.getEntity();
        InputStream responseStream = null;
        String responseString = null;
        JSONObject jsonObject = null;

        responseStream = responseBody.getContent();
        responseString = IOUtils.toString(responseStream, StandardCharsets.UTF_8.name());
        jsonObject = new JSONObject(StringEscapeUtils.unescapeJson(responseString));

        responseStream.close();
        response.close();

        return jsonObject;
    }
}