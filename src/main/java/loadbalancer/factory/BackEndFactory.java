package loadbalancer.factory;

import loadbalancer.BackEnd;
import loadbalancer.monitor.RequestMonitor;

public interface BackEndFactory {
    // produces and returns a backend instance given a request monitor
    BackEnd produceBackEnd(RequestMonitor reqMonitor);

    // produces and returns a backend thread instance given a backend instance
    Thread produceBackEndThread(BackEnd backEnd);
}
