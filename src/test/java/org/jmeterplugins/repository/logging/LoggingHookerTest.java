package org.jmeterplugins.repository.logging;

import kg.apc.emulators.TestJMeterUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.PluginManagerTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertFalse;

public class LoggingHookerTest {

    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
        URL url = PluginManagerTest.class.getResource("/testVirtualPlugin.json");
        JMeterUtils.setProperty("jpgc.repo.address", url.getFile());
    }

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