package loadbalancerlab.cacheservermanager;

import loadbalancerlab.shared.Config;
import loadbalancerlab.shared.Logger;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.ImmutableHttpProcessor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class CacheInfoServerRunnable implements Runnable {
    volatile private int port;
    CacheInfoRequestHandler cacheInfoRequestHandler;
    private static int defaultPort = -1;

    public CacheInfoServerRunnable(CacheInfoRequestHandler _cacheInfoRequestHandler) {
        cacheInfoRequestHandler = _cacheInfoRequestHandler;
    }

    public static void configure( Config config ) {
        defaultPort = config.getCacheInfoServerDefaultPort();
    }

    public int getPort() {
        return port;
    }

    @Override
    public void run() {
        int chosenPort = defaultPort;
        Logger.log("CacheServerManager.CacheInfoServer | Started CacheInfoServer thread", Logger.LogType.THREAD_MANAGEMENT);
        InetAddress hostAddress = null;

        try {
            hostAddress = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException within CacheServerManager#run");
            e.printStackTrace();
        }

        SocketConfig config = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        HttpServer server;

        while (true) {
            System.out.println("Attempting to start server on port " + chosenPort);
            try {
                server = ServerBootstrap.bootstrap()
                        .setLocalAddress(hostAddress)
                        .setListenerPort(chosenPort)
                        .setHttpProcessor(new ImmutableHttpProcessor(new ArrayList<>(), new ArrayList<>()))
                        .setSocketConfig(config)
                        .registerHandler("/cache-servers", cacheInfoRequestHandler)
                        .create();

                server.start();
            } catch(IOException e) {
                System.out.println("IOException within CacheServerManager#run");
                System.out.println("Failed to start server on selected port. Trying another port");
                chosenPort++;
                continue;
            }

            // break out of loop if server successfully started
            System.out.println("CacheServerManager | Server successfully started on port " + chosenPort);
            break;
        }

        this.port = chosenPort;

        HttpServer finalServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                finalServer.shutdown(5, TimeUnit.SECONDS);
            }
        });

        try {
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Logger.log("CacheInfoServer | CacheInfoServer thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
        } finally {
            // shutdown server
            Thread.currentThread().interrupt();
            server.shutdown(5, TimeUnit.SECONDS);
        }

        Logger.log("CacheInfoServer | Shut down CacheInfoServer", Logger.LogType.THREAD_MANAGEMENT);
    }
}