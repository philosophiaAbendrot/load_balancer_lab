package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.shared.Logger;
import loadbalancerlab.shared.RequestDecoder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class CacheInfoRequestHandlerTest {
    HttpClientFactory clientFactory;
    static final int DEFAULT_SERVER_PORT = 8_080;
    static int serverPort;
    static Thread dummyServerThread;
    static RequestDecoder reqDecoder;
    static ServerMonitor mockServerMonitor;
    static Map<Integer, ServerInfo> mockServerInfoTable;
    static double cf1 = 0.55;
    static double cf2 = 0.85;
    static int port1 = 10_105;
    static int port2 = 10_106;
    static int serverId1 = 1;
    static int serverId2 = 2;

    @BeforeAll
    public static void beforeAll() {
        mockServerMonitor = Mockito.mock(ServerMonitor.class);
        mockServerInfoTable = new HashMap<>();
        ServerInfo mockServerInfo1 = Mockito.mock(ServerInfo.class);
        ServerInfo mockServerInfo2 = Mockito.mock(ServerInfo.class);
        when(mockServerInfo1.getCurrentCapacityFactor()).thenReturn(cf1);
        when(mockServerInfo2.getCurrentCapacityFactor()).thenReturn(cf2);
        when(mockServerInfo1.getPort()).thenReturn(port1);
        when(mockServerInfo2.getPort()).thenReturn(port2);
        when(mockServerInfo1.getServerId()).thenReturn(serverId1);
        when(mockServerInfo2.getServerId()).thenReturn(serverId2);
        mockServerInfoTable.put(serverId1, mockServerInfo1);
        mockServerInfoTable.put(serverId2, mockServerInfo2);

        when(mockServerMonitor.getServerInfo()).thenReturn(mockServerInfoTable);
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
        dummyServerThread = new Thread(new DummyServer(mockServerMonitor));
        dummyServerThread.start();
        reqDecoder = new RequestDecoder();
    }

    @BeforeEach
    public void setup() {
        this.clientFactory = new HttpClientFactory();
    }

    @AfterAll
    public static void shutdownServer() {
        dummyServerThread.interrupt();
    }

    private static class DummyServer implements Runnable {
        HttpServer server;
        ServerMonitor serverMonitor;

        public DummyServer( ServerMonitor _serverMonitor) {
            serverMonitor = _serverMonitor;
        }

        @Override
        public void run() {
            serverPort = DEFAULT_SERVER_PORT;

            InetAddress hostAddress;

            try {
                hostAddress = InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }

            HttpProcessor httpProcessor = new ImmutableHttpProcessor(new ArrayList<>(), new ArrayList<>());

            SocketConfig config = SocketConfig.custom()
                    .setSoTimeout(15_000)
                    .setTcpNoDelay(true)
                    .build();

            while (true) {
                server = ServerBootstrap.bootstrap()
                        .setLocalAddress(hostAddress)
                        .setListenerPort(serverPort)
                        .setHttpProcessor(httpProcessor)
                        .setSocketConfig(config)
                        .registerHandler("/cache-servers", new CacheInfoRequestHandler(serverMonitor))
                        .create();

                try {
                    server.start();
                } catch (IOException e) {
                    serverPort++;
                    continue;
                }

                break;
            }

            final HttpServer finalServer = server;

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    finalServer.shutdown(1, TimeUnit.SECONDS);
                }
            });

            try {
                server.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                server.shutdown(1, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    @DisplayName("When a request is sent for server info, information is returned in JSON format")
    public void requestServerInfoReturnsJSONResponse() throws IOException {
        // send request to server, see what happens
        CloseableHttpClient client = clientFactory.buildApacheClient();
        HttpGet req = new HttpGet("http://127.0.0.1:" + serverPort + "/cache-servers");
        CloseableHttpResponse response = client.execute(req);
        JSONObject responseJson = reqDecoder.extractJsonApacheResponse(response);
        JSONObject serverInfo1 = responseJson.getJSONObject(String.valueOf(serverId1));
        int port1Actual = serverInfo1.getInt("port");
        double cf1Actual = serverInfo1.getDouble("capacityFactor");
        JSONObject serverInfo2 = responseJson.getJSONObject(String.valueOf(serverId2));
        int port2Actual = serverInfo2.getInt("port");
        double cf2Actual = serverInfo2.getDouble("capacityFactor");

        assertEquals(port1, port1Actual);
        assertEquals(cf1, cf1Actual);
        assertEquals(port2, port2Actual);
        assertEquals(cf2, cf2Actual);
    }
}