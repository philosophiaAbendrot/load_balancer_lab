package loadbalancerlab.cacheservermanager;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.util.Logger;
import loadbalancerlab.util.RequestDecoder;
import loadbalancerlab.util.RequestDecoderImpl;
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
    static CacheServerManager dummyCacheServerManager;
    static Map<Integer, ServerInfo> mockServerInfoTable;

    @BeforeAll
    public static void beforeAllTests() {
        dummyCacheServerManager = Mockito.mock(CacheServerManager.class);
        mockServerInfoTable = new HashMap<>();

        ServerInfo info1 = new ServerInfo(1, 10_105);
        info1.capacityFactor = 0.55;
        ServerInfo info2 = new ServerInfo(2, 10_106);
        info2.capacityFactor = 0.19;
        mockServerInfoTable.put(1, info1);
        mockServerInfoTable.put(2, info2);

        when(dummyCacheServerManager.getServerInfo()).thenReturn(mockServerInfoTable);
        Logger.configure(new Logger.LogType[] { Logger.LogType.PRINT_NOTHING });
        dummyServerThread = new Thread(new DummyServer(dummyCacheServerManager));
        dummyServerThread.start();
        reqDecoder = new RequestDecoderImpl();
    }

    @BeforeEach
    public void setup() {
        this.clientFactory = new HttpClientFactoryImpl();
    }

    @AfterAll
    public static void shutdownServer() {
        System.out.println("interruption logic running");
        dummyServerThread.interrupt();
    }

    private static class DummyServer implements Runnable {
        HttpServer server;
        CacheServerManager cacheServerManager;

        public DummyServer(CacheServerManager _cacheServerManager) {
            cacheServerManager = _cacheServerManager;
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

            System.out.println("hostAddress = " + hostAddress);
            System.out.println("dummyServerPort = " + DEFAULT_SERVER_PORT);

            while (true) {
                server = ServerBootstrap.bootstrap()
                        .setLocalAddress(hostAddress)
                        .setListenerPort(serverPort)
                        .setHttpProcessor(httpProcessor)
                        .setSocketConfig(config)
                        .registerHandler("/cache-servers", new CacheInfoRequestHandler(cacheServerManager))
                        .create();

                try {
                    server.start();
                } catch (IOException e) {
                    System.out.println("LoadBalancer | Failed to start server on port " + serverPort++);
                    continue;
                }

                System.out.println("LoadBalancer | Started server on port " + serverPort);
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
        System.out.println("sending a test request");
        CloseableHttpClient client = clientFactory.buildApacheClient();
        HttpGet req = new HttpGet("http://127.0.0.1:" + serverPort + "/cache-servers");
        CloseableHttpResponse response = client.execute(req);
        JSONObject responseJson = reqDecoder.extractJsonApacheResponse(response);
        JSONObject serverInfo1 = responseJson.getJSONObject("1");
        int port = serverInfo1.getInt("port");
        double cf = serverInfo1.getDouble("capacityFactor");

        assertEquals(port, 10_105);
        assertEquals(cf, 0.55);
    }
}