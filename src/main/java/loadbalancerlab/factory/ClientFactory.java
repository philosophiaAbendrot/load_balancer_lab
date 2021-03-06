package loadbalancerlab.factory;

import loadbalancerlab.client.Client;
import loadbalancerlab.client.DemandFunction;
import loadbalancerlab.shared.RequestDecoder;

public class ClientFactory {
    public Client buildClient( long maxDemandTime, DemandFunction demandFunc, HttpClientFactory httpClientFactory, long requestStartTime, RequestDecoder reqDecoder ) {
        return new Client(maxDemandTime, demandFunc, httpClientFactory, requestStartTime, reqDecoder);
    }
}