package org.jmeterplugins.repository.logging;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class LoggingConfiguratorTest {

    @Test
    public void testFlow() throws Exception {
        LoggingConfigurator configurator = new LoggingConfigurator();
        Logger log = LoggingManager.getLoggerForClass();
        log.info("Hi, logger");
        assertNotNull(configurator);
    }
}