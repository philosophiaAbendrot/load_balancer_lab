package loadbalancerlab.shared;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Logger {
    // list of log types (not exhaustive)
    // threadManagment = starting and shutting down threads
    // loadModulation = recording load on each server and adjusting server capacity
    // recordingData = recording simulation data and synthesizing it

    private static Set<LogType> displayedLogTypes;

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
        ALWAYS_PRINT,
        PRINT_NOTHING,
        STARTUP_SEQUENCE,
    }

    public static void configure(LogType[] types) {
        displayedLogTypes = new HashSet<>(Arrays.asList(types));
    }
    public static void log(String msg, LogType type) {
        if (type == LogType.PRINT_NOTHING)
            return;

        if (displayedLogTypes.contains(type) || type == LogType.ALWAYS_PRINT)
            System.out.printf("%s | %s\n", msg, java.time.ZonedDateTime.now());
    }
}
