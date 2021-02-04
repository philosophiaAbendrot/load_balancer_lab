import java.util.ArrayList;
import java.util.List;

public class Run {
    final static int NUM_CLIENTS = 1;
    long maxDemandTime;
    final static int CLIENT_REQUEST_SEND_TIME = 40_000;
    final static int STARTUP_SERVER_COUNT = 1;

    public Run() {
        maxDemandTime = System.currentTimeMillis() + 20_000;
    }

    public void start() {
        Logger.configure(new String[] { "threadManagement", "loadModulation" });
        Logger.log("Run | started Run thread", "threadManagement");
        Thread loadBalancerThread = new Thread(new LoadBalancer(8080, STARTUP_SERVER_COUNT));
        Thread backendInitiatorThread = new Thread(new BackEndInitiator());
        List<Thread> clients = new ArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            Thread clientThread = new Thread(new Client(Integer.toString(i), this.maxDemandTime));
            clients.add(clientThread);
        }

        loadBalancerThread.start();
        backendInitiatorThread.start();

        for (Thread clientThread : clients)
            clientThread.start();

        // send requests from clients
        try {
            Thread.sleep(CLIENT_REQUEST_SEND_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // shutdown client threads
        for (Thread client: clients)
            client.interrupt();

        Logger.log("Run | shutdown stage 1: shutdown client threads", "threadManagement");

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // shutdown load balancer
        loadBalancerThread.interrupt();

        Logger.log("Run | shutdown stage 2: Shutdown LoadBalancer thread", "threadManagement");

        try {
            Thread.sleep(5_000);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        // shutdown backendInitiator
        backendInitiatorThread.interrupt();
        Logger.log("Run | shutdown stage 3: Shutdown BackendInitiator thread", "threadManagement");
        Logger.log("Run | terminated Run thread", "threadManagement");
    }

    public static void main(String[] args) {
        new Run().start();
    }
}
