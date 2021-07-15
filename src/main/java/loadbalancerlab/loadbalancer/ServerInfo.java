package loadbalancerlab.loadbalancer;

/**
 * A data class which is used to store information on CacheServer objects for the loadbalancerlab.loadbalancer package.
 */
public class ServerInfo {

    /**
     * The id of the CacheServer that this object concerns.
     */
    int serverId;

    /**
     * The port that the CacheServer is running on.
     */
    int port;

    /**
     * The capacity factor of the CacheServer instance which measures the load on it.
     * This is calculated as the proportion of a recent timespan that the server has spent processing a request.
     */
    double cf;

    /**
     * Constructor
     * @param serverId      The id of the CacheServer this object concerns.
     * @param port          the port that the CacheServer is running on.
     * @param cf            The current capacity factor of the CacheServer.
     */
    public ServerInfo( int serverId, int port, double cf ) {
        this.serverId = serverId;
        this.port = port;
        this.cf = cf;
    }

    /**
     * Getter and Setter methods
     */

    /**
     * @return      The id of the CacheServer.
     */
    public int getServerId() {
        return serverId;
    }

    /**
     * @return      The port that the CacheServer is running on.
     */
    public int getPort() {
        return port;
    }

    /**
     * @return      The capacity factor of the CacheServer.
     */
    public double getCapacityFactor() {
        return cf;
    }

    /**
     * @param cf    The capacity factor of the CacheServer object.
     */
    public void setCapacityFactor( double cf ) {
        this.cf = cf;
    }
}