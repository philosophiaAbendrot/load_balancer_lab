package loadbalancerlab.cacheservermanager;

import java.util.SortedMap;
import java.util.TreeMap;

public class ServerInfoImpl implements ServerInfo {
    private int serverId;
    private int port;
    private SortedMap<Integer, Double> capFactorRecord;
    int cfRecordSize = 10;

    public ServerInfoImpl( int _serverId, int _port ) {
        this.serverId = _serverId;
        this.port = _port;
        capFactorRecord = new TreeMap<>();
    }

    @Override
    public SortedMap<Integer, Double> getCapacityFactorRecord() {
        SortedMap<Integer, Double> copyMap = (SortedMap<Integer, Double>) ((TreeMap<Integer, Double>)capFactorRecord).clone();
        return copyMap;
    }

    @Override
    public double getAverageCapacityFactor() {
        double totalCf = 0;

        for (Double cf : capFactorRecord.values()) {
            totalCf += cf;
        }

        return totalCf / capFactorRecord.size();
    }

    @Override
    public int getServerId() {
        return serverId;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort( int _port ) {
        port = _port;
    }

    @Override
    public void setServerId( int _serverId ) {
        serverId = _serverId;
    }

    @Override
    public void updateCapacityFactor( int timestamp, double cf ) {
        if (capFactorRecord.containsKey(timestamp)) {
            // do nothing if an entry already exists for this timestamp
            return;
        }

        // prevent cap factor record size from increasing past cfRecordSize
        if (capFactorRecord.size() >= cfRecordSize) {
            int lowestKey = capFactorRecord.firstKey();
            capFactorRecord.remove(lowestKey);
        }

        // add new entry
        capFactorRecord.put(timestamp, cf);
    }
}