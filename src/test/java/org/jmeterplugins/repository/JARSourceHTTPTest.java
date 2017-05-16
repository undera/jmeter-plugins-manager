package org.jmeterplugins.repository;

import org.junit.Test;


import static org.junit.Assert.*;

public class JARSourceHTTPTest {

    @Test
    public void testInstallId() throws Exception {
        JARSourceHTTP source = new JARSourceHTTP("");
        assertEquals(source.getPlatformName(), 1, 3);
    }
}