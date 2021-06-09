package loadbalancerlab.loadbalancer;

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

public class ClientRequestHandlerServer implements Runnable {
    volatile private static int defaultPort;
    volatile private int port = -1;
    public int getPort() {
        return port;
    }
    private Logger logger;
    LoadBalancerClientRequestHandler loadBalancerClientRequestHandler;

    public static void configure( Config config ) {
        defaultPort = config.getClientHandlerServerDefaultPort();
    }

    public ClientRequestHandlerServer( LoadBalancerClientRequestHandler loadBalancerClientRequestHandler ) {
        this.loadBalancerClientRequestHandler = loadBalancerClientRequestHandler;
        logger = new Logger("ClientRequestHandlerServer");
    }

    @Override
    public void run() {
        logger.log("ClientRequestHandlerServer thread started", Logger.LogType.THREAD_MANAGEMENT);
        InetAddress hostAddress = null;

        try {
            hostAddress = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException Within LoadBalancer#run");
            e.printStackTrace();
        }

        SocketConfig config = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        HttpServer server;
        int temporaryPort = defaultPort;

        while (true) {
            server = ServerBootstrap.bootstrap()
                    .setLocalAddress(hostAddress)
                    .setListenerPort(temporaryPort)
                    .setHttpProcessor(new ImmutableHttpProcessor(new ArrayList<>(), new ArrayList<>()))
                    .setSocketConfig(config)
                    .registerHandler("/api/*", loadBalancerClientRequestHandler)
                    .create();

            try {
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("ClientRequestHandler | Failed to start server on port " + temporaryPort);
                temporaryPort++;
                continue;
            }

            // if server successfully started, exit the loop
            this.port = temporaryPort;
            break;
        }

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
            logger.log("Thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
        } finally {
            Thread.currentThread().interrupt();
            server.shutdown(5, TimeUnit.SECONDS);
            // shut down capacity factor monitor thread
            // shut down this thread
            logger.log("Thread shutdown", Logger.LogType.THREAD_MANAGEMENT);
        }
    }
}