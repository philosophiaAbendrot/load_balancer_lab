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
        BackEndFactory mockFactory = Mockito.mock(BackEndFactoryImpl.class);
        BackEnd mockBackEnd = Mockito.mock(BackEnd.class);
        mockBackEnd.port = 37100;
        Thread mockBackEndThread = Mockito.mock(Thread.class);

        when(mockFactory.produceBackEnd(any(RequestMonitor.class))).thenReturn(mockBackEnd);
        when(mockFactory.produceBackEndThread(any(BackEnd.class))).thenReturn(new Thread(mockBackEndThread));

        BackEndInitiator initiator = new BackEndInitiator(mockFactory);
        Thread initiatorThread = new Thread(initiator);
        initiatorThread.start();

        int backEndInitiatorPort = waitUntilServerReady(initiator);

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

    @Test
    @DisplayName("When BackEndInitiator thread is interrupted, it interrupts all backend servers that it has spawned")
    public void BackEndInitiatorThreadInterruptedInterruptsAllBackEndServers() {
        BackEndFactory mockFactory = Mockito.mock(BackEndFactoryImpl.class);
        BackEnd mockBackEnd = Mockito.mock(BackEnd.class);
        mockBackEnd.port = 37100;
        Thread mockThread = Mockito.mock(Thread.class);

        when(mockFactory.produceBackEnd(any(RequestMonitor.class))).thenReturn(mockBackEnd);
        when(mockFactory.produceBackEndThread(any(BackEnd.class))).thenReturn(mockThread);

        BackEndInitiator initiator = new BackEndInitiator(mockFactory);
        Thread initiatorThread = new Thread(initiator);
        initiatorThread.start();

        int backEndInitiatorPort = waitUntilServerReady(initiator);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost req = new HttpPost("http://127.0.0.1:" + backEndInitiatorPort + "/backends");

        // send request to server and wait for it to be received
        try {
            CloseableHttpResponse response = client.execute(req);
            client.close();
            Thread.sleep(100);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // interrupt BackEndInitiator thread
        initiatorThread.interrupt();

        // wait for BackEndInitiator to run interruption callbacks
        try {
            Thread.sleep(100);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        // verify that BackEndThread has been interrupted
        verify(mockThread, times(1)).interrupt();
    }

    // waits until a server has started up
    // returns port
    private int waitUntilServerReady(BackEndInitiator initiator) {
        int backEndInitiatorPort = initiator.getPort();

        while (backEndInitiatorPort == -1) {
            try {
                Thread.sleep(20);
                backEndInitiatorPort = initiator.getPort();
            } catch (InterruptedException e) { }
        }

        return backEndInitiatorPort;
    }
}