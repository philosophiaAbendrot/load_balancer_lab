package loadbalancerlab.cacheserver;

import loadbalancerlab.shared.Logger;
import com.sun.net.httpserver.HttpServer;
import loadbalancerlab.shared.RequestDecoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ClientRequestHandlerTest {
    TestServer testServer;
    Thread testServerThread;
    RequestMonitor mockReqMonitor = Mockito.mock(RequestMonitor.class);
    ClientRequestHandler clientReqHandler;
    int testServerPort;
    String resourceName;
    RequestDecoder reqDecoder;
    CloseableHttpResponse res;

    class TestServer implements Runnable {
        int[] selectablePorts;
        RequestMonitor reqMonitor;
        volatile public int port;
        ClientRequestHandler clientReqHandler;

        public TestServer(RequestMonitor _reqMonitor, ClientRequestHandler _clientReqHandler) {
            reqMonitor = _reqMonitor;
            clientReqHandler = _clientReqHandler;
        }

        @Override
        public void run() {
            ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            HttpServer server = null;
            initializeSelectablePorts();

            for (int i = 0; i < selectablePorts.length; i++) {
                port = selectablePorts[i];
                System.out.println("attempting to start test server");

                try {
                    InetAddress host = InetAddress.getByName("127.0.0.1");
                    InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                    server = HttpServer.create(socketAddress, 0);
                    server.createContext("/", clientReqHandler);
                    server.setExecutor(threadPool);
                    break;
                } catch(IOException e) {
                    System.out.println("Failed to start CacheServer on port " + port);
                }
            }

            server.start();
            System.out.println("server started on port " + port);

            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    server.stop(1);
                    threadPool.shutdown();
                    break;
                }
            }

            System.out.println("shutdown test server");
        }

        private void initializeSelectablePorts() {
            selectablePorts = new int[100];

            for (int i = 0; i < selectablePorts.length; i++)
                selectablePorts[i] = 37100 + i;
        }
    }

    @BeforeEach
    public void setup() {
        Logger.configure(new Logger.LogType[] { Logger.LogType.REQUEST_PASSING });

        reqDecoder = new RequestDecoder();
        resourceName = "Chooder_Bunny.jpg";
        clientReqHandler = new ClientRequestHandler(mockReqMonitor);
        testServer = new TestServer(mockReqMonitor, clientReqHandler);
        testServerThread = new Thread(testServer);
        testServerThread.start();

        // wait until test server started up and selected a port
        while (testServer.port == 0) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) { }
        }

        testServerPort = testServer.port;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet getReq = new HttpGet("http://127.0.0.1:" + testServerPort + "/" + resourceName);

        try {
            res = client.execute(getReq);
        } catch (IOException e) { }
    }

    @AfterEach
    public void shutdown() {
        testServerThread.interrupt();
    }

    @Test
    @DisplayName("ClientRequestHandler should call 'addServer' method on RequestMonitor instance")
    public void shouldCallAddServer() {
        verify(mockReqMonitor, times(1)).addRecord(anyLong(), anyLong());
    }

    @Nested
    @DisplayName("Test that response is correct")
    class TestResponse {
        JSONObject resJson;

        @BeforeEach
        public void setup() {
            try {
                resJson = reqDecoder.extractJsonApacheResponse(res);
            } catch (IOException e) { }
        }

        @Test
        @DisplayName("response should contain resource name within uri")
        public void responseShouldContainResourceName() {
            String resName = resJson.getString("resourceName");
            assertEquals(resourceName, resName);
        }

        @Test
        @DisplayName("response should contain resource contents")
        public void responseShouldContainResourceContents() {
            String resContents = resJson.getString("resourceContents");
            assertEquals("here it is", resContents);
        }
    }
}