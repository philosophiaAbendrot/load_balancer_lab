package loadbalancer.factory;

import loadbalancer.BackEnd;
import loadbalancer.monitor.RequestMonitor;

public class BackEndFactoryImpl implements BackEndFactory {
    public BackEnd produceBackEnd(RequestMonitor reqMonitor) {
        return new BackEnd(reqMonitor);
    }

    public Thread produceBackEndThread(BackEnd backEnd) {
        return new Thread(backEnd);
    }
}