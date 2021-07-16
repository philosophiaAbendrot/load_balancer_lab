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

/**
 * A server which handles requests for CacheInfo telemetry from the LoadBalancerRunnable class using the
 * CacheInfoRequestHandler class.
 */
public class CacheInfoServerRunnable implements Runnable {

    /**
     * Stores the port that the CacheInfoServer is running on.
     */
    volatile private int port;

    /**
     * The associated request handler which is employed by the server to handle requests.
     */
    CacheInfoRequestHandler cacheInfoRequestHandler;

    /**
     * The default port that the server attempts to start on.
     */
    private static int defaultPort = -1;

    /**
     * Object used for logging.
     */
    private Logger logger;

    /**
     * Constructor
     * @param cacheInfoRequestHandler   CacheInfoRequestHandler object which contains logic to handle HttpRequest from
     *                                  LoadBalancerRunnable for update on CacheServer telemetry.
     */
    public CacheInfoServerRunnable( CacheInfoRequestHandler cacheInfoRequestHandler ) {
        this.cacheInfoRequestHandler = cacheInfoRequestHandler;
        logger = new Logger("CacheInfoServerRunnable");
    }

    /**
     * Used to configure class fields.
     * @param config: An Object used to store configurations for various classes.
     */
    public static void configure( Config config ) {
        defaultPort = config.getCacheInfoServerDefaultPort();
    }

    /**
     * Getter method for the port this object is running on.
     * @return the port object is running on
     */
    public int getPort() {
        return port;
    }

    /**
     * Runs and manages lifecycle of CacheInfoServer.
     * Method from Runnable interface.
     */
    @Override
    public void run() {
        int chosenPort = defaultPort;
        logger.log("Started CacheInfoServer thread", Logger.LogType.THREAD_MANAGEMENT);
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

        /* Attempt to start server on 'defaultPort'. If that fails, continue trying to start the server on higher port
           numbers */
        while (true) {
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

            /* Break out of loop if server successfully started */
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

            /* Await termination of thread */
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.log("CacheInfoServerRunnable thread interrupted", Logger.LogType.THREAD_MANAGEMENT);
        } finally {

            /* Shutdown server */
            Thread.currentThread().interrupt();
            server.shutdown(5, TimeUnit.SECONDS);
        }

        logger.log("Shut down CacheInfoServerRunnable thread", Logger.LogType.THREAD_MANAGEMENT);
    }
}