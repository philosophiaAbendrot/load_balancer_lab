package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ServerMonitorTest {
    HttpClientFactory clientFactory;
    CloseableHttpClient mockClient;
    int currentTime;
    ServerMonitor serverMonitor;
    CacheServerManager mockCacheServerManager;
    RequestDecoder mockDecoder;

    @BeforeAll
    public static void beforeAll() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
    }

    @BeforeEach
    public void setup() {
        this.clientFactory = Mockito.mock(HttpClientFactory.class);
        this.mockClient = Mockito.mock(CloseableHttpClient.class);
        this.mockDecoder = Mockito.mock(RequestDecoder.class);
        this.mockCacheServerManager = Mockito.mock(CacheServerManager.class);
    }

    @Nested
    @DisplayName("Testing deliverData() and updateServerCount()")
    public class TestAnalytics {
        @BeforeEach
        public void setup() {
            serverMonitor = new ServerMonitor(clientFactory, mockDecoder, mockCacheServerManager);
        }

        @Test
        @DisplayName("Should return the correct number of active servers by second")
        public void returnActiveServers() {
            currentTime = (int)System.currentTimeMillis() / 1_000;
            serverMonitor.updateServerCount(currentTime + 1, 10);
            serverMonitor.updateServerCount(currentTime + 2, 12);
            serverMonitor.updateServerCount(currentTime + 3, 15);
            SortedMap<Integer, Integer> serverData = serverMonitor.deliverServerCountData();

            assertTrue(serverData.containsKey(currentTime + 1));
            assertTrue(serverData.containsKey(currentTime + 2));
            assertTrue(serverData.containsKey(currentTime + 3));
            assertEquals(serverData.get(currentTime + 1), 10);
            assertEquals(serverData.get(currentTime + 2), 12);
            assertEquals(serverData.get(currentTime + 3), 15);
        }

        @Test
        @DisplayName("If there are duplicate entries for a given second, the first one should be recorded")
        public void ignoreDuplicateInputs() {
            currentTime = (int)System.currentTimeMillis() / 1_000;
            serverMonitor.updateServerCount(currentTime + 1, 10);
            serverMonitor.updateServerCount(currentTime + 2, 12);
            serverMonitor.updateServerCount(currentTime + 2, 16);
            serverMonitor.updateServerCount(currentTime + 3, 15);
            SortedMap<Integer, Integer> serverData = serverMonitor.deliverServerCountData();

            assertTrue(serverData.containsKey(currentTime + 2));
            assertEquals(serverData.get(currentTime + 2), 12);
        }
    }

    @Nested
    @DisplayName("Tests pingCacheServers() method")
    class TestPingCacheServers {
        ArgumentCaptor<HttpUriRequest> argument;
        int cacheServerPort;
        JSONObject mockJsonResponse;
        CloseableHttpResponse mockResponse;
        int numServers = 3;

        @BeforeEach
        public void setup() throws IOException {
            mockCacheServerManager = Mockito.mock(CacheServerManager.class);
            this.argument = ArgumentCaptor.forClass(HttpGet.class);

            // set up canned response regarding the port of the server that the cache server was started up on which is returned when a request is made through the
            // mock HttpClient to the (virtual) CacheServerManager to start a server
            this.cacheServerPort = 1_000;
            this.mockJsonResponse = new JSONObject();
            this.mockJsonResponse.put("port", this.cacheServerPort);
            this.mockJsonResponse.put("capacity_factor", 0.5);
            when(clientFactory.buildApacheClient()).thenReturn(mockClient);
            this.mockResponse = Mockito.mock(CloseableHttpResponse.class);
            when(mockClient.execute(any(HttpUriRequest.class))).thenReturn(this.mockResponse);
            when(mockDecoder.extractJsonApacheResponse(any(CloseableHttpResponse.class))).thenReturn(this.mockJsonResponse);
            serverMonitor = new ServerMonitor(clientFactory, mockDecoder, mockCacheServerManager);

            // add server info instances to monitor
            for (int i = 0; i < numServers; i++)
                serverMonitor.addServer(i, 10_000 + i);

            serverMonitor.pingCacheServers();
        }

        @Test
        @DisplayName("Test that request is sent to CacheServer for update on capacity factor")
        public void shouldSendRequestWithPostMethod() throws IOException {
            verify(mockClient, times(numServers)).execute(any(HttpGet.class));
        }

        @Test
        @DisplayName("Request should be of type POST")
        public void shouldSendRequestToCacheServerManager() throws IOException {
            verify(mockClient, times(numServers)).execute(this.argument.capture());
            HttpUriRequest request = this.argument.getAllValues().get(1);
            assertEquals("GET", request.getMethod());
        }

        @Test
        @DisplayName("Request should go to correct URI")
        public void requestShouldGoToCorrectURI() throws IOException {
            verify(mockClient, times(numServers)).execute(this.argument.capture());
            HttpUriRequest request1 = this.argument.getAllValues().get(0);
            assertEquals("http://127.0.0.1:" + 10000 + "/capacity-factor", request1.getURI().toString());
            HttpUriRequest request2 = this.argument.getAllValues().get(1);
            assertEquals("http://127.0.0.1:" + 10001 + "/capacity-factor", request2.getURI().toString());
            HttpUriRequest request3 = this.argument.getAllValues().get(2);
            assertEquals("http://127.0.0.1:" + 10002 + "/capacity-factor", request3.getURI().toString());
        }

        @Test
        @DisplayName("Should update its server info based on received capacity factor information")
        public void shouldUpdateInfo() throws IOException {
            List<ServerInfo> infoList = new ArrayList<>(serverMonitor.serverInfoTable.values());
            assertEquals(0.5, infoList.get(0).getCurrentCapacityFactor());
        }
    }

    @Nested
    @DisplayName("Tests addServer()")
    class AddServer {
        @BeforeEach
        public void setup() {
            serverMonitor = new ServerMonitor(clientFactory, mockDecoder, mockCacheServerManager);
        }

        @Test
        @DisplayName("Adding server should add a new entry to serverInfoTable")
        public void shouldAddNewEntryToTable() {
            serverMonitor.addServer(1, 10_015);
            serverMonitor.addServer(2, 10_030);
            assertTrue(serverMonitor.serverInfoTable.containsKey(1));
            assertTrue(serverMonitor.serverInfoTable.containsKey(2));
            ServerInfo info1 = serverMonitor.serverInfoTable.get(1);
            ServerInfo info2 = serverMonitor.serverInfoTable.get(2);
            assertEquals(info1.getServerId(), 1);
            assertEquals(info2.getServerId(), 2);
            assertEquals(info1.getPort(), 10_015);
            assertEquals(info2.getPort(), 10_030);
        }

        @Test
        @DisplayName("Adding server with an existing id should raise an error")
        public void shouldRaiseErrorWhenAddingDuplicateEntry() {
            serverMonitor.addServer(1, 10_015);
            assertThrows(IllegalArgumentException.class, () -> {
                serverMonitor.addServer(1, 13_581);
            });
        }
    }

    @Nested
    @DisplayName("Tests getAverageCf()")
    class TestGetAverageCf {
        @BeforeEach
        public void setup() {
            Random rand = new Random();

            serverMonitor = new ServerMonitor(clientFactory, mockDecoder, mockCacheServerManager);
            serverMonitor.addServer(1, 10_105);
            serverMonitor.addServer(2, 37_594);
            serverMonitor.addServer(3, 14_049);

            currentTime = (int)(System.currentTimeMillis() / 1_000);

            // add entries to server info table
            for (int i = 1; i < 4; i++) {
                for (int j = 0; j < 10; j++) {
                    serverMonitor.serverInfoTable.get(i).updateCapacityFactor(currentTime - 10 + j, rand.nextDouble());
                }
            }
        }

        @Test
        @DisplayName("Should return correct average capacity factor")
        public void shouldReturnCorrectAvgCf() {
            double expectedCfSum = 0;

            for (int i = 1; i < 4; i++) {
                expectedCfSum += serverMonitor.serverInfoTable.get(i).getCurrentCapacityFactor();
            }

            double expectedCf = expectedCfSum / 3;
            assertTrue(Math.abs(expectedCf - serverMonitor.getAverageCf()) < 0.001);
        }
    }

    @Nested
    @DisplayName("Tests deliverCfData()")
    class TestsDeliverCfData {
        String[][] result;
        int serverId1 = 10691, serverId2 = 10805;
        int port1 = 69205, port2 = 80290;
        ServerInfo info1, info2;
        int indexTime;

        @BeforeEach
        public void setup() {
            serverMonitor = new ServerMonitor(clientFactory, mockDecoder, mockCacheServerManager);

            ConcurrentMap<Integer, ServerInfo> serverInfoTable = new ConcurrentHashMap<>();
            info1 = new ServerInfo(serverId1, port1);
            info2 = new ServerInfo(serverId2, port2);
            indexTime = (int)(System.currentTimeMillis() / 1_000);

            // setup capacity factor history
            info1.updateCapacityFactor( indexTime - 5, 0.65 );
            info1.updateCapacityFactor(indexTime - 4, 0.95);
            info1.updateCapacityFactor(indexTime - 3, 0.26);
            info1.updateCapacityFactor(indexTime - 2, 0.74);
            info1.updateCapacityFactor(indexTime - 1, 0.58);

            // setup capacity factor history
            info2.updateCapacityFactor(indexTime - 5, 0.59);
            info2.updateCapacityFactor(indexTime - 4, 0.84);
            info2.updateCapacityFactor(indexTime - 3, 0.39);
            info2.updateCapacityFactor(indexTime - 2, 0.62);
            info2.updateCapacityFactor(indexTime - 1, 0.51);

            serverInfoTable.put(serverId1, info1);
            serverInfoTable.put(serverId2, info2);
            serverMonitor.serverInfoTable = serverInfoTable;

            result = serverMonitor.deliverCfData();
        }

        @Test
        @DisplayName("topmost row should contain all server ids")
        public void topRowShouldContainServerIds() {
            assertEquals(String.valueOf(serverId1), result[0][1]);
            assertEquals(String.valueOf(serverId2), result[0][2]);
        }

        @Test
        @DisplayName("leftmost column should contain all timestamps")
        public void leftColumnShouldContainTimestamps() {
            assertEquals(String.valueOf(indexTime - 5), result[1][0]);
            assertEquals(String.valueOf(indexTime - 4), result[2][0]);
            assertEquals(String.valueOf(indexTime - 3), result[3][0]);
            assertEquals(String.valueOf(indexTime - 2), result[4][0]);
        }

        @Test
        @DisplayName("Should return data on capacity factor of cache servers at each moment in time")
        public void shouldReturnDataOnCapacityFactorOfCacheServers() {
            assertEquals(String.valueOf(0.26), result[3][1]);
        }
    }
}