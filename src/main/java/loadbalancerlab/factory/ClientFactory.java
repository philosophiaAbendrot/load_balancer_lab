package loadbalancerlab.factory;

import loadbalancerlab.client.Client;
import loadbalancerlab.client.DemandFunction;

public class ClientFactory {
    public Client buildClient( long maxDemandTime, DemandFunction demandFunc, HttpClientFactory httpClientFactory, long requestStartTime ) {
        return new Client(maxDemandTime, demandFunc, httpClientFactory, requestStartTime);
    }
}