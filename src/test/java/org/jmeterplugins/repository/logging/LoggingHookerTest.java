package org.jmeterplugins.repository.logging;

import org.jmeterplugins.repository.PluginManager;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class LoggingHookerTest {

    /**
     * For logging in JMeter 2.13-3.1
     */
    @Test
    public void testFlowOld() throws Exception {
        LoggingHooker hooker = new LoggingHooker(new PluginManager());
        hooker.hook();
        assertFalse(hooker.isJMeter32orLater());
    }


}