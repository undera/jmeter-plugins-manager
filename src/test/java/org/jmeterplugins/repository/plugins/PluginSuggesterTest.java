package org.jmeterplugins.repository.plugins;

import kg.apc.emulators.TestJMeterUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.PluginManagerTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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
            PluginManager pmgr = new PluginManager();
            pmgr.load();

            PluginSuggester suggester = new PluginSuggester(pmgr);
            suggester.checkAndSuggest("Loading file : " + testPlan.getPath());
        }
    }

    @Test
    public void testSuggest() throws Throwable {
        URL repo = PluginManagerTest.class.getResource("/suggest.json");
        JMeterUtils.setProperty("jpgc.repo.address", repo.getPath());
        PluginManager pmgr = new PluginManager();
        pmgr.load();


        PluginSuggester suggester = new PluginSuggester(pmgr);
        TestPlanAnalyzer analyzer = new TestPlanAnalyzer();
        suggester.setAnalyzer(analyzer);
        assertEquals(analyzer, suggester.getAnalyzer());

        suggester.togglePlugins(pmgr.getAvailablePlugins());
        String msg = suggester.generateMessage(pmgr.getAvailablePlugins());

        assertTrue(msg.contains("Dependency plugin1"));
        assertTrue(msg.contains("Dependency plugin2"));

        suggester.notify("123%");
        suggester.notify("123");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        JMeterUtils.getJMeterProperties().remove("jpgc.repo.address");
    }
}