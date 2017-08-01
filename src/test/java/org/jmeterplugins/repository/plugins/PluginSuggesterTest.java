package org.jmeterplugins.repository.plugins;

import kg.apc.emulators.TestJMeterUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.PluginManagerTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.net.URL;


public class PluginSuggesterTest {
    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
    }


    @Test
    public void testFlow() throws Throwable {
        if (!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            URL repo = PluginManagerTest.class.getResource("/suggest.json");
            URL testPlan = PluginManagerTest.class.getResource("/testplan.xml");

            JMeterUtils.setProperty("jpgc.repo.address", repo.getPath());
            PluginManager pmgr = PluginManager.getStaticManager();
            pmgr.load();

            PluginSuggester suggester = new PluginSuggester(pmgr);
            suggester.checkAndSuggest("Loading file : " + testPlan.getPath());
        }
    }
}