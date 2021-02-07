import java.util.*;

public class Run {
    final static int NUM_CLIENTS = 3;
    long maxDemandTime;
    final static int CLIENT_REQUEST_SEND_TIME = 40_000;
    final static int STARTUP_SERVER_COUNT = 3;

    public Run() {
        maxDemandTime = System.currentTimeMillis() + 20_000;
    }

    // start simulation
    public void start() {
        Logger.configure(new String[] { "threadManagement", "loadModulation", "recordingData" });
        Logger.log("Run | started Run thread", "threadManagement");
        LoadBalancer loadBalancer = new LoadBalancer(8080, STARTUP_SERVER_COUNT);
        Thread loadBalancerThread = new Thread(loadBalancer);
        BackEndInitiator backendInitiator = new BackEndInitiator();
        Thread backendInitiatorThread = new Thread(backendInitiator);
        List<Thread> clientThreads = new ArrayList<>();
        List<Client> clients = new ArrayList<>();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            Client client = new Client(Integer.toString(i), this.maxDemandTime);
            Thread clientThread = new Thread(client);
            clients.add(client);
            clientThreads.add(clientThread);
        }

        loadBalancerThread.start();
        backendInitiatorThread.start();

        for (Thread clientThread : clientThreads)
            clientThread.start();

        // send requests from clients
        try {
            Thread.sleep(CLIENT_REQUEST_SEND_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SortedMap<Integer, Integer> synthesizedClientRequestLog = new TreeMap<>();

        // shutdown client threads and synthesize data from the client servers
        for (int i = 0; i < clientThreads.size(); i++) {
            Thread clientThread = clientThreads.get(i);
            Client client = clients.get(i);
            SortedMap<Integer, Integer> clientRequestLog = client.deliverData();

            for (Map.Entry<Integer, Integer> entry : clientRequestLog.entrySet()) {
                if (synthesizedClientRequestLog.containsKey(entry.getKey())) {
                    // if entry exists, increment
                    Integer prev = synthesizedClientRequestLog.get(entry.getKey());
                    synthesizedClientRequestLog.put(entry.getKey(), prev + 1);
                } else {
                    // otherwise, create new entry
                    synthesizedClientRequestLog.put(entry.getKey(), entry.getValue());
                }
            }

            // initiate client server shutdown
            clientThread.interrupt();
        }

        Logger.log("Run | SynthesizedClientRequestLog:", "recordingData");

        // printout synthesized client server request data
        for (Map.Entry<Integer, Integer> entry : synthesizedClientRequestLog.entrySet())
            Logger.log(String.format("%d | %d", entry.getKey(), entry.getValue()), "recordingData");

        Logger.log("Run | shutdown stage 1: shutdown client threads", "threadManagement");

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // collect data from load balancer
        Logger.log("collecting request log data from load balancer", "recording data");
        SortedMap<Integer, Integer> loadBalancerRequestLog = loadBalancer.deliverData();

        // printout load balancer request data
        Logger.log("Run | loadBalancerRequestLog:", "recordingData");

        for (Map.Entry<Integer, Integer> entry : loadBalancerRequestLog.entrySet())
            Logger.log(String.format("%d | %d", entry.getKey(), entry.getValue()), "recordingData");

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
