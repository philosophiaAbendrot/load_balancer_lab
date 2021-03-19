import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.apache.http.impl.client.HttpClients;
import org.mockito.Mockito;

import loadbalancer.BackEndInitiator;
import loadbalancer.factory.BackEndFactoryImpl;
import loadbalancer.factory.BackEndFactory;
import loadbalancer.util.Logger;
import loadbalancer.monitor.RequestMonitor;
import loadbalancer.BackEnd;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BackEndInitiatorTest {
    @Test
    @DisplayName("BackEndInitiator should start up a BackEnd instance when sent a request telling it to do so")
    public void BackEndInitiatorShouldStartBackEndInstance() {
        Logger.configure(new String[]{"capacityModulation"});
        BackEndFactory mockFactory = Mockito.mock(BackEndFactoryImpl.class);
        BackEnd backEnd = new BackEnd(new RequestMonitor("BackEndInitiatorTest"));

        when(mockFactory.produceBackEnd(any(RequestMonitor.class))).thenReturn(backEnd);
        when(mockFactory.produceBackEndThread(any(BackEnd.class))).thenReturn(new Thread(backEnd));

        BackEndInitiator initiator = new BackEndInitiator(mockFactory);
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
            CloseableHttpResponse response = client.execute(req);
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        verify(mockFactory, times(1)).produceBackEnd(any(RequestMonitor.class));
        verify(mockFactory, times(1)).produceBackEndThread(any(BackEnd.class));
    }

    // backend initiator should shutdown a backend server when sent a request telling it to do so
}