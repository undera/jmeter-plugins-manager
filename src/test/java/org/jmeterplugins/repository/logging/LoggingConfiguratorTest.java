package org.jmeterplugins.repository.logging;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

public class LoggingConfiguratorTest {

    @Test
    public void testFlow() throws Exception {
        LoggingConfigurator configurator = new LoggingConfigurator();
        Logger log = LoggerFactory.getLogger(LoggingConfiguratorTest.class);
        log.info("Hi, logger");
        assertNotNull(configurator);
    }
}