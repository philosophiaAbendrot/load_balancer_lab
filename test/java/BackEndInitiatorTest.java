import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
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
    BackEndFactory mockFactory;
    BackEnd mockBackEnd;
    Thread mockBackEndThread;
    BackEndInitiator initiator;
    Thread initiatorThread;
    int backEndInitiatorPort;

    @BeforeEach
    public void setup() {
        this.mockFactory = Mockito.mock(BackEndFactoryImpl.class);
        this.mockBackEnd = Mockito.mock(BackEnd.class);
        this.mockBackEnd.port = 37_100;
        this.mockBackEndThread = Mockito.mock(Thread.class);

        when(this.mockFactory.produceBackEnd(any(RequestMonitor.class))).thenReturn(mockBackEnd);
        when(this.mockFactory.produceBackEndThread(any(BackEnd.class))).thenReturn(this.mockBackEndThread);

        this.initiator = new BackEndInitiator(this.mockFactory);
        this.initiatorThread = new Thread(this.initiator);
        this.initiatorThread.start();
        this.backEndInitiatorPort = waitUntilServerReady(this.initiator);
    }

    @Test
    @DisplayName("BackEndInitiator should start up a BackEnd instance when sent a request telling it to do so")
    public void BackEndInitiatorShouldStartBackEndInstance() {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost req = new HttpPost("http://127.0.0.1:" + this.backEndInitiatorPort + "/backends");

        try {
            CloseableHttpResponse response = client.execute(req);
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        verify(this.mockFactory, times(1)).produceBackEnd(any(RequestMonitor.class));
        verify(this.mockFactory, times(1)).produceBackEndThread(any(BackEnd.class));
    }

    @Test
    @DisplayName("When BackEndInitiator thread is interrupted, it interrupts all backend servers that it has spawned")
    public void BackEndInitiatorThreadInterruptedInterruptsAllBackEndServers() {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost req = new HttpPost("http://127.0.0.1:" + this.backEndInitiatorPort + "/backends");

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
        this.initiatorThread.interrupt();

        // wait for BackEndInitiator to run interruption callbacks
        try {
            Thread.sleep(100);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        // verify that BackEndThread has been interrupted
        verify(this.mockBackEndThread, times(1)).interrupt();
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