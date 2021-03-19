import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.apache.http.impl.client.HttpClients;

import loadbalancer.BackEndInitiator;
import loadbalancer.factory.BackEndFactoryImpl;
import loadbalancer.factory.BackEndFactory;
import loadbalancer.util.Logger;
import org.mockito.Mockito;

import java.io.IOException;

public class BackEndInitiatorTest {
    @Test
    @DisplayName("BackEndInitiator should start up a BackEnd instance when sent a request telling it to do so")
    public void BackEndInitiatorShouldStartBackEndInstance() {
        Logger.configure(new String[]{"capacityModulation"});

        BackEndFactory factory = Mockito.mock(BackEndFactoryImpl.class);
        BackEndInitiator initiator = new BackEndInitiator(factory);
        Thread initiatorThread = new Thread(initiator);
        initiatorThread.start();

        int backEndInitiatorPort = initiator.getPort();

        while (backEndInitiatorPort == -1) {
            try {
                backEndInitiatorPort = initiator.getPort();
                Thread.sleep(20);
            } catch (InterruptedException e) { }
        }

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost req = new HttpPost("http://127.0.0.1:" + backEndInitiatorPort + "/backends");

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception thrown while waiting for BackEndInitiator to start up");
            e.printStackTrace();
        }

        try {
            System.out.println("sending request to BackEndInitiator");
            CloseableHttpResponse response = client.execute(req);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // send request to BackEndInitiator
    private void sendRequest() {

    }

    // backend initiator should shutdown a backend server when sent a request telling it to do so
}