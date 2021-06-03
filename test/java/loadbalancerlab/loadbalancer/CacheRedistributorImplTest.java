package loadbalancerlab.loadbalancer;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.ConfigImpl;
import loadbalancerlab.shared.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CacheRedistributorImplTest {
    Config config;
    CacheRedistributorImpl cacheRedis;
    int cacheInfoServerPort = 8080;

    @Nested
    @DisplayName("Test RequestServerInfo()")
    class TestRequestServerInfo {
        // contacts cache server monitor and records data to serverInfoTable
        HttpClientFactory mockClientFactory = Mockito.mock(HttpClientFactory.class);
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = Mockito.mock(CloseableHttpResponse.class);
        RequestDecoder mockDecoder = Mockito.mock(RequestDecoder.class);
        JSONObject mockJsonResponse = Mockito.mock(JSONObject.class);
        JSONObject mockNestedJson3 = Mockito.mock(JSONObject.class);
        JSONObject mockNestedJson4 = Mockito.mock(JSONObject.class);

        double targetCapacityFactor = 0.5;
        double cf3 = 0.48;
        double cf4 = 0.56;

        @BeforeEach
        public void setup() throws IOException {
            // configuration logic
            config = new ConfigImpl();
            // setup config
            config.setRequestDecoder(mockDecoder);
            config.setTargetCapacityFactor(targetCapacityFactor);
            config.setClientFactory(mockClientFactory);
            CacheRedistributorImpl.configure(config);

            // setting up mocks
            when(mockNestedJson3.getDouble("capacityFactor")).thenReturn(cf3);
            when(mockNestedJson4.getDouble("capacityFactor")).thenReturn(cf4);
            when(mockJsonResponse.getJSONObject("3")).thenReturn(mockNestedJson3);
            when(mockJsonResponse.getJSONObject("4")).thenReturn(mockNestedJson4);
            Set<String> mockKeySet = new HashSet<>();
            mockKeySet.add("3");
            mockKeySet.add("4");
            when(mockJsonResponse.keySet()).thenReturn(mockKeySet);
            when(mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(mockJsonResponse);
            when(mockClientFactory.buildApacheClient()).thenReturn(mockClient);
            when(mockClient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
            when(mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(mockJsonResponse);

            cacheRedis = new CacheRedistributorImpl(cacheInfoServerPort, new HashRingImpl());
        }

        @Nested
        @DisplayName("For servers that don't exist in server info table")
        class WhenInfoDoesNotExistInRecord {
            @BeforeEach
            public void setup() {
                cacheRedis.requestServerInfo();
            }

            @Test
            @DisplayName("should record the capacity factor into the JSON response into the associated ServerInfo instance")
            public void shouldRecordCapacityFactorIntoJsonResponse() {
                assertEquals(cf3, cacheRedis.serverInfoTable.get(3).getCapacityFactor());
                assertEquals(cf4, cacheRedis.serverInfoTable.get(4).getCapacityFactor());
            }
        }

        @Nested
        @DisplayName("For servers that are recorded in server info table")
        class WhenInfoDoesExistInRecord {
            ServerInfo serverInfo3;
            ServerInfo serverInfo4;
            int serverPort3 = 3681;
            int serverPort4 = 4959;
            double cf3Initial = 0.589;
            double cf4Initial = 0.11;

            @BeforeEach
            public void setup() {
                serverInfo3 = new ServerInfoImpl(3, serverPort3, cf3Initial);
                serverInfo4 = new ServerInfoImpl(4, serverPort4, cf4Initial);
                cacheRedis.serverInfoTable.put(3, serverInfo3);
                cacheRedis.serverInfoTable.put(4, serverInfo4);
                cacheRedis.requestServerInfo();
            }

            @Test
            @DisplayName("should record the capacity factor into the JSON response into the associated ServerInfo instance")
            public void shouldRecordCapacityFactorIntoJsonResponse() {
                assertEquals(cf3, cacheRedis.serverInfoTable.get(3).getCapacityFactor());
                assertEquals(cf4, cacheRedis.serverInfoTable.get(4).getCapacityFactor());
            }
        }
    }

    @Nested
    @DisplayName("Test selectPort()")
    class TestSelectPort {
        HashRing mockHashRing;
        String resourceName = "Chooder_Bunny";
        int selectedPort;
        int port1 = 10_105;
        double cf1 = 0.44;
        int port2 = 6_820;
        double cf2 = 0.81;
        int selectedServerId = 1;

        @BeforeEach
        public void setup() {
            // configuration
            config = new ConfigImpl();
            CacheRedistributorImpl.configure(config);

            // setting up mocks
            mockHashRing = Mockito.mock(HashRingImpl.class);
            when(mockHashRing.findServerId(anyString())).thenReturn(selectedServerId);

            // initialization
            cacheRedis = new CacheRedistributorImpl(cacheInfoServerPort, mockHashRing);
            cacheRedis.serverInfoTable = new HashMap<>();
            cacheRedis.serverInfoTable.put(1, new ServerInfoImpl(1, port1, cf1));
            cacheRedis.serverInfoTable.put(2, new ServerInfoImpl(2, port2, cf2));

            selectedPort = cacheRedis.selectPort(resourceName);
        }

        @Test
        @DisplayName("should return correct server id")
        public void testServer() {
            int expectedPort = cacheRedis.serverInfoTable.get(selectedServerId).getPort();
            assertEquals(expectedPort, selectedPort);
        }
    }

    @Nested
    @DisplayName("Test remapCacheKeys()")
    class TestRemapCacheKeys {
        HashRing mockHashRing;
        int port1 = 4_810, port2 = 5_848, port3 = 5198, port4 = 8931, port5 = 1185;
        double cf1 = 0.1, cf2 = 0.2, cf3 = 0.4, cf4 = 0.75, cf5 = 0.9;

        @BeforeEach
        public void setup() {
            // configuration
            config = new ConfigImpl();
            CacheRedistributorImpl.configure(config);

            // initialization
            mockHashRing = Mockito.mock(HashRing.class);
            cacheRedis = new CacheRedistributorImpl(cacheInfoServerPort, mockHashRing);
            cacheRedis.serverInfoTable = new HashMap<>();
            cacheRedis.serverInfoTable.put(1, new ServerInfoImpl(1, port1, cf1));
            cacheRedis.serverInfoTable.put(2, new ServerInfoImpl(2, port2, cf2));
            cacheRedis.serverInfoTable.put(3, new ServerInfoImpl(3, port3, cf3));
            cacheRedis.serverInfoTable.put(4, new ServerInfoImpl(4, port4, cf4));
            cacheRedis.serverInfoTable.put(5, new ServerInfoImpl(5, port5, cf5));
        }

        @Test
        @DisplayName("Should modulate number of angles in hash ring by adding angles")
        public void shouldAddAnglesWhereRequired() {
            cacheRedis.remapCacheKeys();

            // checking arguments
            ArgumentCaptor<Integer> args1 = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Integer> args2 = ArgumentCaptor.forClass(Integer.class);

            verify(mockHashRing, times(2)).addAngle(args1.capture(), args2.capture());

            List<Integer> values1 = args1.getAllValues();
            List<Integer> values2 = args2.getAllValues();

            Map<Integer, Integer> argsHash = new HashMap<>();

            for (int i = 0; i < values1.size(); i++) {
                argsHash.put(values1.get(i), values2.get(i));
            }

            assertEquals(argsHash.get(1), 3);
            assertEquals(argsHash.get(2), 1);
        }

        @Test
        @DisplayName("Should modulate number of angles in hash ring by removing angles")
        public void shouldRemoveAnglesWhereRequired() {
            cacheRedis.remapCacheKeys();

            // checking arguments
            ArgumentCaptor<Integer> args1 = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Integer> args2 = ArgumentCaptor.forClass(Integer.class);
            // setting up mocks
            verify(mockHashRing, times(2)).removeAngle(args1.capture(), args2.capture());


            List<Integer> values1 = args1.getAllValues();
            List<Integer> values2 = args2.getAllValues();

            Map<Integer, Integer> argsHash = new HashMap<>();

            for (int i = 0; i < values1.size(); i++) {
                argsHash.put(values1.get(i), values2.get(i));
            }

            assertEquals(argsHash.get(4), 1);
            assertEquals(argsHash.get(5), 3);
        }
    }
}