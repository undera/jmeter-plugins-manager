package org.jmeterplugins.repository.logging;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.PluginManager;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoggingHookerTest {

    /**
     * For logging in JMeter 2.13-3.1
     */
    @Test
    public void testFlowOld() throws Exception {
        LoggingHooker hooker = new LoggingHooker(PluginManager.getStaticManager());
        hooker.hook();
        assertFalse(hooker.isJMeter32orLater());
    }


}