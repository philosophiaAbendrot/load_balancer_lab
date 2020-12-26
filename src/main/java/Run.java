import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Run {
    final static int NUM_CLIENTS = 20;

    public static void main(String[] args) {
        Thread loadBalancerThread = new Thread(new LoadBalancer(8080)); 
        Thread backendInitiatorThread = new Thread(new BackEndInitiator());
        List<Thread> clients = new ArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            Thread clientThread = new Thread(new Client(Integer.toString(i)));
            clients.add(clientThread);
        }

        loadBalancerThread.start();
        backendInitiatorThread.start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        for (Thread clientThread : clients) {
            clientThread.start();
        }
    }
}
