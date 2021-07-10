package loadbalancerlab.shared;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.loadbalancer.HashFunction;
import loadbalancerlab.loadbalancer.MurmurHashFunctionImpl;

/**
 * Used to configure various classes throughout this project
 */
public class Config {
    /**
     * The maximum number of HashRingAngle objects that a CacheServer object can have
     */
    private int maxAnglesPerServer;

    /**
     * The minimum number of HashRingAngle objects that a CacheServer object can own
     */
    private int minAnglesPerServer;

    /**
     * The default number of HashRingAngle objects that a CacheServer is assigned
     */
    private int defaultAnglesPerServer;

    /**
     * The number of positions in a HashRing object
     */
    private int ringSize;

    /**
     * The HashFunction object which is used to hash resource names into integers.
     * Used within a consistent hashing mechanism for assigning resource names to CacheServer objects
     */
    private HashFunction hashFunction;

    /**
     * RequestDecoder object used to extract JSON parameters from a CloseableHttpResponse object
     */
    private RequestDecoder reqDecoder;

    /**
     * Factory object used for generating CloseableHttpClient instances for sending Http requests
     */
    private HttpClientFactory httpClientFactory;

    /**
     * An array of doubles which is used to decide how to modulate the number of HashRingAngle objects a CacheServer
     * object has.
     *
     * The array has 4 elements.
     *
     * If the CacheServer's cf (capacity factor) is less than the first element, it will be given 3 more HashRingAngles.
     * Otherwise, if the cf is less than the second element, it will be given 1 more HashRingAngle.
     * Otherwise, if the cf is larger than the third element, it will have HashRingAngle taken away.
     * Otherwise, if the cf is larger than the last element, it will have 3 HashRingAngles taken away.
     */
    private double[] serverLoadCutoffs;

    /**
     * The minimum interval (in seconds) between calls of CacheRedistributor.requestServerInfo().
     * Controls how often the CacheRedistributor object (within the loadbalancer package) package requests an update from the
     * CacheServerManager object (within the cacheservermanager package) about information on the CacheServers.
     */
    private int cacheRedisPingInterval;

    /**
     * The minimum interval (in seconds) between calls of CacheRedistributor.remapCacheKeys().
     * Controls how often the HashRingAngles are dynamically remapped on the CacheServers to even out the capacity factors
     * of CacheServer objects.
     */
    private int cacheRedisRemapInterval;

    private int clientHandlerServerDefaultPort;
    private int cacheInfoServerDefaultPort;
    private double targetCf;
    private double cacheServerGrowthRate;
    private int capacityModulationInterval;
    private int requestMonitorRecordTTL;
    private int cacheServerProcessingTime;
    private int numClients;
    private int numCacheServersOnStartup;
    private int simulationTime;
    private int hashRingAngleRecordInterval;

    public Config() {

        /* Default values for configurations */
        maxAnglesPerServer = 40;
        minAnglesPerServer = 10;
        defaultAnglesPerServer = 20;
        ringSize = 10_000;
        hashFunction = new MurmurHashFunctionImpl();
        targetCf = 0.5;
        reqDecoder = new RequestDecoder();
        httpClientFactory = new HttpClientFactory();
        serverLoadCutoffs = new double[] { 0.15, 0.35, 0.65, 0.85 };
        cacheRedisPingInterval = 1;
        cacheRedisRemapInterval = 3;
        clientHandlerServerDefaultPort = 3_000;
        cacheInfoServerDefaultPort = 5_500;
        cacheServerGrowthRate = 50;
        capacityModulationInterval = 5;
        requestMonitorRecordTTL = 10_000;
        cacheServerProcessingTime = 200;
        numClients = 20;
        numCacheServersOnStartup = 5;
        simulationTime = 80_000;
        hashRingAngleRecordInterval = 5;

        /* Factories */
        httpClientFactory = new HttpClientFactory();

        /* Other services */
        reqDecoder = new RequestDecoder();
    }

    /* GETTER METHODS */
    public int getMaxAnglesPerServer() {
        return maxAnglesPerServer;
    }

    public int getMinAnglesPerServer() {
        return minAnglesPerServer;
    }

    public int getDefaultAnglesPerServer() {
        return defaultAnglesPerServer;
    }

    public int getRingSize() {
        return ringSize;
    }

    public HashFunction getHashFunction() {
        return hashFunction;
    }

    public RequestDecoder getRequestDecoder() {
        return reqDecoder;
    }

    public HttpClientFactory getHttpClientFactory() {
        return httpClientFactory;
    }

    public double[] getServerLoadCutoffs() {
        return serverLoadCutoffs;
    }

    public int getCacheRedisPingInterval() {
        return cacheRedisPingInterval;
    }

    public int getCacheRedisRemapInterval() {
        return cacheRedisRemapInterval;
    }

    public int getClientHandlerServerDefaultPort() {
        return clientHandlerServerDefaultPort;
    }

    public int getCacheInfoServerDefaultPort() { return cacheInfoServerDefaultPort; }

    public int getCacheServerProcessingTime() { return cacheServerProcessingTime; }

    public double getTargetCf() { return targetCf; }

    public double getCacheServerGrowthRate() { return cacheServerGrowthRate; }

    public int getCapacityModulationInterval() { return capacityModulationInterval; }

    public int getRequestMonitorRecordTTL() { return requestMonitorRecordTTL; }

    public int getNumClients() { return numClients; }

    public int getNumCacheServersOnStartup() { return numCacheServersOnStartup; }

    public int getSimulationTime() { return simulationTime; }

    public int getHashRingAngleRecordInterval() { return hashRingAngleRecordInterval; }

    /* SETTER METHODS */
    public void setMaxAnglesPerServer( int _maxAnglesPerServer ) {
        maxAnglesPerServer = _maxAnglesPerServer;
    }

    public void setMinAnglesPerServer( int _minAnglesPerServer ) {
        minAnglesPerServer = _minAnglesPerServer;
    }

    public void setDefaultAnglesPerServer( int _defaultAnglesPerServer ) {
        defaultAnglesPerServer = _defaultAnglesPerServer;
    }

    public void setRingSize( int _ringSize ) {
        ringSize = _ringSize;
    }

    public void setHashFunction( HashFunction _hashFunction ) {
        hashFunction = _hashFunction;
    }

    public void setRequestDecoder( RequestDecoder _reqDecoder ) {
        reqDecoder = _reqDecoder;
    }

    public void setHttpClientFactory( HttpClientFactory _clientFactory ) {
        httpClientFactory = _clientFactory;
    }

    public void setServerLoadCutoffs( double[] cutoffs ) {
        serverLoadCutoffs = cutoffs;
    }

    public void setCacheRedisPingInterval( int pingInterval ) {
        cacheRedisPingInterval = pingInterval;
    }

    public void setCacheRedisRemapInterval( int remapInterval ) {
        cacheRedisRemapInterval = remapInterval;
    }

    public void setClientHandlerServerDefaultPort( int defaultPort ) {
        clientHandlerServerDefaultPort = defaultPort;
    }

    public void setCacheInfoServerDefaultPort( int defaultPort ) { cacheInfoServerDefaultPort = defaultPort; }

    public void setTargetCf( double cf ) {
        targetCf = cf;
    }

    public void setCacheServerGrowthRate( double growthRate ) { cacheServerGrowthRate = growthRate; }

    public void setCapacityModulationInterval( int modulationInterval ) { capacityModulationInterval = modulationInterval; }

    public void setRequestMonitorRecordTTL( int ttl ) { requestMonitorRecordTTL = ttl; }

    public void setCacheServerProcessingTime( int processingTime ) { cacheServerProcessingTime = processingTime; }

    public void setNumClients(int numClients) { this.numClients = numClients; }

    public void setSimulationTime(int simulationTime) { this.simulationTime = simulationTime; }

    public void setNumCacheServersOnStartup( int numCacheServersOnStartup ) {
        this.numCacheServersOnStartup = numCacheServersOnStartup;
    }

    public void setHashRingAngleRecordInterval(int val) { hashRingAngleRecordInterval = val; }
}