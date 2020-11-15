import java.util.concurrent.TimeUnit;

public class Run {
    public static void main(String[] args) {
        Thread backendThread = new Thread(new BackEnd(6666));
        Thread loadBalancerThread = new Thread(new LoadBalancer(8080));
        Thread clientThread = new Thread(new Client());
        backendThread.start();
        loadBalancerThread.start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch(InterruptedException e) {
            e.getMessage();
        }

        clientThread.start();
        System.out.println("started client");
    }
}
