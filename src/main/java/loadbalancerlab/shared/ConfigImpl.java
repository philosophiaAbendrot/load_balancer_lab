package loadbalancerlab.shared;

public class ConfigImpl implements Config {
    // hash ring configuration
    private int maxAnglesPerServer;
    private int minAnglesPerServer;
    private int defaultAnglesPerServer;
    private int ringSize;

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
}