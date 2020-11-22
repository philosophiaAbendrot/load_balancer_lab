public class Logger {
    public static void log(String msg) {
        System.out.printf("%s | %s\n", msg, java.time.ZonedDateTime.now());
    }
}
