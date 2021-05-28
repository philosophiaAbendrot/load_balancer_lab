package loadbalancerlab.util;

import java.util.HashSet;
import java.util.Set;

public class Logger {
    // list of log types (not exhaustive)
    // threadManagment = starting and shutting down threads
    // loadModulation = recording load on each server and adjusting server capacity
    // recordingData = recording simulation data and synthesizing it

    private static Set<LogType> displayedLogTypes = new HashSet<>();

    public enum LogType {
        THREAD_MANAGEMENT,
        LOAD_MODULATION,
        RECORDING_DATA,
        REQUEST_PASSING,
        CLIENT_STARTUP,
        CAPACITY_MODULATION,
        CACHE_SERVER_STARTUP,
        TELEMETRY_UPDATE,
        LOAD_BALANCER_STARTUP,
        ALWAYS_PRINT
    }

    public static void configure(LogType[] types) {
        for (int i = 0; i < types.length; i++)
            displayedLogTypes.add(types[i]);
    }
    public static void log(String msg, LogType type) {
        if (displayedLogTypes.contains(type) || type == LogType.ALWAYS_PRINT)
            System.out.printf("%s | %s\n", msg, java.time.ZonedDateTime.now());
    }
}
