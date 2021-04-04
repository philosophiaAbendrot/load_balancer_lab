import loadbalancer.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ClientTest {
    @Nested
    @DisplayName("Test request sent to Load Balancer")
    private class TestRequestSentToLoadBalancer {
        @Test
        @DisplayName("Request should be sent to the right uri")
        public void testShouldSendToCorrectUri() {

        }

        @Test
        @DisplayName("Request should be of type GET")
        public void testShouldHaveCorrectMethod() {

        }
    }
}
