import java.util.concurrent.TimeUnit;

public class Run {
    public static void main(String[] args) {
        Thread alphaBackendThread = new Thread(new BackEnd(6666));
        Thread betaBackendThread = new Thread(new BackEnd(4000));
        Thread loadBalancerThread = new Thread(new LoadBalancer(8080));
        Thread clientThread = new Thread(new Client());
        alphaBackendThread.start();
        betaBackendThread.start();
        loadBalancerThread.start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        clientThread.start();
        System.out.println("started client");
    }
}
