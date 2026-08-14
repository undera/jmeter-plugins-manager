package org.jmeterplugins.repository.logging;

import org.jmeterplugins.repository.JMeterTestEnv;
import org.apache.jmeter.util.JMeterUtils;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.PluginManagerTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotNull;

public class LoggingHookerTest {

    @BeforeClass
    public static void setup() {
        JMeterTestEnv.createJMeterEnv();
        URL url = PluginManagerTest.class.getResource("/testVirtualPlugin.json");
        JMeterUtils.setProperty("jpgc.repo.address", url.getFile());
    }

    @Test
    public void testHooksLog4jAppender() throws Exception {
        LoggingHooker hooker = new LoggingHooker(new PluginManager());
        hooker.hook();
        assertNotNull(org.apache.logging.log4j.LogManager.getContext(false));
    }
}
