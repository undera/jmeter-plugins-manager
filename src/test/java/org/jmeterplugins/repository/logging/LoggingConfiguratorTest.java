package org.jmeterplugins.repository.logging;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class LoggingConfiguratorTest {

    @Test
    public void testFlow() throws Exception {
        File logFile = new File("PluginManagerCMD.log");
        if (logFile.exists()) {
            logFile.delete();
        }

        new LoggingConfigurator();
        Logger log = LoggingManager.getLoggerForClass();
        log.info("Hi, logger");

        assertTrue(logFile.exists());
        logFile.delete();
    }
}