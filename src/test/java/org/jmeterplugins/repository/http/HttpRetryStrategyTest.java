package org.jmeterplugins.repository.http;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;

import static org.junit.Assert.*;

public class HttpRetryStrategyTest {

    @Test
    public void testFlow() throws Exception {
        HttpRetryStrategy strategy = new HttpRetryStrategy();
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP",1,1), 200, ""));
        assertFalse(strategy.retryRequest(response, 1, null));

        strategy = new HttpRetryStrategy(2, 3333);
        response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP",1,1), 201, ""));
        assertTrue(strategy.retryRequest(response, 1, null));
        assertFalse(strategy.retryRequest(response, 4, null));

        assertEquals(2, strategy.getMaxRetries());
        assertEquals(3333, strategy.getRetryInterval());

        try {
            new HttpRetryStrategy(-1, 2222);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("MaxRetries must be greater than 1", ex.getMessage());
        }


        try {
            new HttpRetryStrategy(2, -1);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Retry interval must be greater than 1", ex.getMessage());
        }


    }
}