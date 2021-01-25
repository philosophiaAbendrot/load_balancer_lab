import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Run {
    final static int NUM_CLIENTS = 5;

    public static void main(String[] args) {
        Logger.configure(new String[] { "threadManagement" });
        Logger.log("Run | started Run thread", "threadManagement");
//        Logger.configure(new String[] {"telemetryUpdate", "capacityModulation"});
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

        // shutdown backendInitiator
        backendInitiatorThread.interrupt();

        for (Thread clientThread : clients) {
            clientThread.start();
        }

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Thread client: clients) {
            // shutdown client threads
            client.interrupt();
        }

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // shutdown load balancer
        loadBalancerThread.interrupt();

        Logger.log("Run | terminated Run thread", "threadManagement");
    }
}
