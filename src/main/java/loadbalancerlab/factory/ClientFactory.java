package loadbalancerlab.factory;

import loadbalancerlab.client.Client;
import loadbalancerlab.client.DemandFunction;
import loadbalancerlab.shared.RequestDecoder;

/**
 * A factory class for producing Client instances.
 */
public class ClientFactory {

    /**
     * Produces a client instance for generating request traffic to LoadBalancer instances.
     * @param maxDemandTime: The timestamp at which the demand function peaks (irrelevant for constant demand functions).
     * @param demandFunc: DemandFunction instance which controls the request load put out by the Client as a function
     *                    of time.
     * @param httpClientFactory: Factory class used to generate CloseableHttpClient instances.
     * @param requestStartTime: The timestamp at which the Client instance will start sending requests.
     * @param reqDecoder: Used to extract JSON parameters from CloseableHttpResponse instances.
     * @return: Returns a Client instance.
     */
    public Client buildClient( long maxDemandTime, DemandFunction demandFunc, HttpClientFactory httpClientFactory, long requestStartTime, RequestDecoder reqDecoder ) {
        return new Client(maxDemandTime, demandFunc, httpClientFactory, requestStartTime, reqDecoder);
    }
}