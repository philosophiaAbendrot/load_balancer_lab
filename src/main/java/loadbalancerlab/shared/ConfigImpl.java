package loadbalancerlab.shared;

import loadbalancerlab.factory.HttpClientFactory;
import loadbalancerlab.factory.HttpClientFactoryImpl;
import loadbalancerlab.loadbalancer.HashFunction;
import loadbalancerlab.loadbalancer.MurmurHashFunctionImpl;

public class ConfigImpl implements Config {
    private int maxAnglesPerServer;
    private int minAnglesPerServer;
    private int defaultAnglesPerServer;
    private int ringSize;
    private HashFunction hashFunction;
    private double targetCapacityFactor;
    private RequestDecoder reqDecoder;
    private HttpClientFactory clientFactory;
    private double[] serverLoadCutoffs;

    public ConfigImpl() {
        // default configurations
        maxAnglesPerServer = 40;
        minAnglesPerServer = 10;
        defaultAnglesPerServer = 20;
        ringSize = 10_000;
        hashFunction = new MurmurHashFunctionImpl();
        targetCapacityFactor = 0.5;
        reqDecoder = new RequestDecoderImpl();
        clientFactory = new HttpClientFactoryImpl();
        serverLoadCutoffs = new double[] { 0.15, 0.35, 0.65, 0.85 };
    }

    @Override
    public int getMaxAnglesPerServer() {
        return maxAnglesPerServer;
    }

    @Override
    public int getMinAnglesPerServer() {
        return minAnglesPerServer;
    }

    @Override
    public int getDefaultAnglesPerServer() {
        return defaultAnglesPerServer;
    }

    @Override
    public int getRingSize() {
        return ringSize;
    }

    @Override
    public HashFunction getHashFunction() {
        return hashFunction;
    }

    @Override
    public double getTargetCapacityFactor() {
        return targetCapacityFactor;
    }

    @Override
    public RequestDecoder getRequestDecoder() {
        return reqDecoder;
    }

    @Override
    public HttpClientFactory getClientFactory() {
        return clientFactory;
    }

    @Override
    public double[] getServerLoadCutoffs() {
        return serverLoadCutoffs;
    }

    @Override
    public void setMaxAnglesPerServer( int _maxAnglesPerServer ) {
        maxAnglesPerServer = _maxAnglesPerServer;
    }

    @Override
    public void setMinAnglesPerServer( int _minAnglesPerServer ) {
        minAnglesPerServer = _minAnglesPerServer;
    }

    @Override
    public void setDefaultAnglesPerServer( int _defaultAnglesPerServer ) {
        defaultAnglesPerServer = _defaultAnglesPerServer;
    }

    @Override
    public void setRingSize( int _ringSize ) {
        ringSize = _ringSize;
    }

    @Override
    public void setHashFunction(HashFunction _hashFunction) {
        hashFunction = _hashFunction;
    }

    @Override
    public void setTargetCapacityFactor( double _targetCapacityFactor ) {
        targetCapacityFactor = _targetCapacityFactor;
    }

    @Override
    public void setRequestDecoder( RequestDecoder _reqDecoder ) {
        reqDecoder = _reqDecoder;
    }

    @Override
    public void setClientFactory( HttpClientFactory _clientFactory ) {
        clientFactory = _clientFactory;
    }

    @Override
    public void setServerLoadCutoffs( double[] cutoffs ) {
        serverLoadCutoffs = cutoffs;
    }
}