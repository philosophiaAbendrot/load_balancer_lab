package loadbalancerlab.client;

import loadbalancerlab.shared.Logger;

/**
 * Sleeps the thread for a specified amount of time.
 * Implementation of DemandFunction interface.
 * A simple linear ramp demand function.
 * Demand increases linearly with time.
 */
public class LinearRampDemandFunctionImpl implements DemandFunction {

    /**
     * The maximum length of sleep interval which is the setting at the beginning of the period.
     */
    private int startRestInterval;

    /**
     * The minimum length of sleep interval which is reached at the end of the period.
     */
    private int endRestInterval;

    /**
     * Object used for running time (milliseconds).
     */
    private int runningTime;

    /**
     * Time at which demand function was initialized.
     */
    private int startTime;

    /**
     * Object used for logging.
     */
    private Logger logger;

    /**
     * Constructor
     * @param startRestInterval     The starting sleep interval between requests (in milliseconds).
     * @param endRestInterval       The sleep interval between requests (in milliseconds), which is reached at the
     *                              end of the period.
     * @param runningTime           The total amount of time (in milliseconds) which the demand function runs for.
     */
    public LinearRampDemandFunctionImpl( int startRestInterval, int endRestInterval, int runningTime, int startTime ) {
        this.startRestInterval = startRestInterval;
        this.endRestInterval = endRestInterval;
        this.runningTime = runningTime;
        this.startTime = startTime;
        logger = new Logger("LinearRampDemandFunctionImpl");
    }

    /**
     * Makes the thread sleep for a certain amount of time.
     * Method from DemandFunction interface.
     * @throws InterruptedException     Thrown when thread is interrupted while the thread is sleeping.
     */
    @Override
    public void rest() throws InterruptedException {
        int currentTime = (int)(System.currentTimeMillis() / 1_000);

        double startDemand = 1 / ((double) startRestInterval);
        double endDemand = 1 / ((double) endRestInterval);

        double demandSlope = (endDemand - startDemand) / ((double)(runningTime / 1000));
        double deltaT = currentTime - startTime;

        /* If time is past the running time, return the ending interval */
        double currentDemand = Math.min(startDemand + demandSlope * (deltaT), endDemand);
        int sleepTime = (int)(1.0 / currentDemand);
        Thread.sleep(sleepTime);
    }
}
