import java.util.concurrent.TimeUnit;

public class Run {
    public static void main(String[] args) {
        Thread loadBalancerThread = new Thread(new LoadBalancer(8080));
        Thread backendInitiatorThread = new Thread(new BackEndInitiator());
        Thread clientThread = new Thread(new Client());
        loadBalancerThread.start();
        backendInitiatorThread.start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        clientThread.start();
    }
}
