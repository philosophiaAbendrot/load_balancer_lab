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
    /**
     * the default port which the server started by an instance of this class tries to run on
     */
    volatile private static int defaultPort;

    /**
     * the port which the server started by an instance of this class is running on
     */
    volatile private int port = -1;

    /**
     * Logger object used for logging
     */
    private Logger logger;

    /**
     * LoadBalancerClientRequestHandler object used for handling requests from clients
     */
    LoadBalancerClientRequestHandler loadBalancerClientRequestHandler;

    /**
     * Method to configure static fields
     * @param config: Config object used to configure various classes
     */
    public static void configure( Config config ) {
        defaultPort = config.getClientHandlerServerDefaultPort();
    }

    public ClientRequestHandlerServer( LoadBalancerClientRequestHandler loadBalancerClientRequestHandler ) {
        this.loadBalancerClientRequestHandler = loadBalancerClientRequestHandler;
        logger = new Logger("ClientRequestHandlerServer");
    }

    /**
     * Runnable interface method
     * Starts an apache.http.impl.bootstrap.HttpServer instance and awaits termination
     */
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

    /**
     * @return: the port an instance of this class is running a server on
     */
    public int getPort() {
        return port;
    }
}