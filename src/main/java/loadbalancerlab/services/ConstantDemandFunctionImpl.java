package loadbalancerlab.services;

import loadbalancerlab.util.Logger;

public class ConstantDemandFunctionImpl implements DemandFunction {
    int restInterval;

    public ConstantDemandFunctionImpl(int restInterval) {
        this.restInterval = restInterval;
    }

    @Override
    public void rest() throws InterruptedException {
        Logger.log("Client | restInterval = " + this.restInterval, Logger.LogType.LOAD_MODULATION);
        Thread.sleep(this.restInterval);
    }

    // alternate demand function
    // -0.0005(x - 20)^2 + 0.5

    //        if (Math.abs(x - maxDemandTime) >= 19500) {
    //            return Integer.MAX_VALUE;
    //        } else {
    //            // demand curve is a downward facing parabola
    //            double delta = (x - maxDemandTime) / 1000.0;
    //            double demand = Math.max(-0.0005 * Math.pow(delta, 2) + 0.5, 0.07);
    //            // introduce variability
    //            Random rand = new Random();
    //            double variabilityFactor = 0.7 +  0.6 * rand.nextDouble();
    //            Logger.log("Client | demand = " + demand, "recordingData");
    //            int waitTime = (int)Math.round(1000 / demand * variabilityFactor);
    //            Logger.log("Client | waitTime = " + waitTime, "recordingData");
    //            return waitTime;
    //        }
}
