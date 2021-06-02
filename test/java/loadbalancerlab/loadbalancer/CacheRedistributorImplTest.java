package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CacheRedistributorImplTest {
    @Nested
    @DisplayName("Test RequestServerInfo()")
    class TestRequestServerInfo {
        // contacts cache server monitor and records data to serverInfoTable
        CacheRedistributorImpl cacheRedis;
        HttpClientFactory mockClientFactory = Mockito.mock(HttpClientFactory.class);
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        RequestDecoder mockDecoder = Mockito.mock(RequestDecoder.class);
        JSONObject mockJsonResponse = Mockito.mock(JSONObject.class);
        JSONObject mockNestedJson3 = Mockito.mock(JSONObject.class);
        JSONObject mockNestedJson4 = Mockito.mock(JSONObject.class);
        int cacheServerManagerPort = 8080;
        double[] initialCf3 = { 0.3, 0.35, 0.4, 0.41, 0.42, 0.4, 0.43, 0.38, 0.42, 0.44, 0.55, 0.49, 0.52 };
        double[] initialCf4 = { 0.6, 0.55, 0.49, 0.55, 0.59, 0.35, 0.49, 0.39, 0.65, 0.52, 0.49, 0.61, 0.55 };

//        @BeforeEach
//        public void setup() {
//            when(mockNestedJson3.getDouble("capacityFactor")).thenReturn(cf3[0]);
//            when(mockNestedJson4.getDouble("capacityFactor")).thenReturn(cf4[0]);
//            when(mockJsonResponse.getJSONObject("3")).thenReturn(mockNestedJson3);
//            when(mockJsonResponse.getJSONObject("4")).thenReturn(mockNestedJson4);
//
//            cacheRedis = new CacheRedistributorImpl(cacheServerManagerPort);
//            cacheRedis.serverInfoTable = new HashMap<>();
//
//            for (int i = 0; i < initialCf3.length; i++) {
//
//            }
//
//            for (int i = 0; i < initialCf4.length; i++) {
//
//            }
//        }

        @Test
        @DisplayName("should record the capacity factor into the JSON response into the associated ServerInfo instance")
        public void shouldRecordCapacityFactorIntoJsonResponse() {

        }
    }
}