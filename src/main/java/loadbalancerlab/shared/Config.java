package loadbalancerlab.shared;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.loadbalancer.HashFunction;
import loadbalancerlab.loadbalancer.MurmurHashFunctionImpl;

public class Config {
    private int maxAnglesPerServer;
    private int minAnglesPerServer;
    private int defaultAnglesPerServer;
    private int ringSize;
    private HashFunction hashFunction;
    private double targetCapacityFactor;
    private RequestDecoder reqDecoder;
    private HttpClientFactory clientFactory;
    private double[] serverLoadCutoffs;
    private int cacheRedisPingInterval;
    private int cacheRedisRemapInterval;
    private int clientHandlerServerDefaultPort;
    private int cacheInfoServerDefaultPort;
    private double targetCf;
    private double cacheServerGrowthRate;
    private int capacityModulationInterval;
    private long requestMonitorTTL;

    public Config() {
        // default configurations
        maxAnglesPerServer = 40;
        minAnglesPerServer = 10;
        defaultAnglesPerServer = 20;
        ringSize = 10_000;
        hashFunction = new MurmurHashFunctionImpl();
        targetCapacityFactor = 0.5;
        reqDecoder = new RequestDecoder();
        clientFactory = new HttpClientFactory();
        serverLoadCutoffs = new double[] { 0.15, 0.35, 0.65, 0.85 };
        cacheRedisPingInterval = 1;
        cacheRedisRemapInterval = 3;
        clientHandlerServerDefaultPort = 3_000;
        cacheInfoServerDefaultPort = 5_500;
        targetCf = 0.75;
        cacheServerGrowthRate = 25;
        capacityModulationInterval = 5;
        requestMonitorTTL = 10_000;
    }

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

    public double getTargetCapacityFactor() {
        return targetCapacityFactor;
    }

    public RequestDecoder getRequestDecoder() {
        return reqDecoder;
    }

    public HttpClientFactory getClientFactory() {
        return clientFactory;
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

    public double getTargetCf() { return targetCf; }

    public double getCacheServerGrowthRate() { return cacheServerGrowthRate; }

    public int getCapacityModulationInterval() { return capacityModulationInterval; }

    public long getRequestMonitorTTL() { return requestMonitorTTL; }

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

    public void setHashFunction(HashFunction _hashFunction) {
        hashFunction = _hashFunction;
    }

    public void setTargetCapacityFactor( double _targetCapacityFactor ) {
        targetCapacityFactor = _targetCapacityFactor;
    }

    public void setRequestDecoder( RequestDecoder _reqDecoder ) {
        reqDecoder = _reqDecoder;
    }

    public void setClientFactory( HttpClientFactory _clientFactory ) {
        clientFactory = _clientFactory;
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

    public void setTargetCf(double cf) {
        targetCf = cf;
    }

    public void setCacheServerGrowthRate(double growthRate) { cacheServerGrowthRate = growthRate; }

    public void setCapacityModulationInterval(int modulationInterval) { capacityModulationInterval = modulationInterval; }

    public void setRequestMonitorTTL(long ttl) { requestMonitorTTL = ttl; }
}