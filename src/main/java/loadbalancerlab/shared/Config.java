package loadbalancerlab.shared;

public interface Config {
    // hash ring configuration
    int getMaxAnglesPerServer();
    int getMinAnglesPerServer();
    int getDefaultAnglesPerServer();
    int getRingSize();

    void setMaxAnglesPerServer(int _maxAnglesPerServer);
    void setMinAnglesPerServer(int _minAnglesPerServer);
    void setDefaultAnglesPerServer(int _defaultAnglesPerServer);
    void setRingSize(int _ringSize);
}