package org.jmeterplugins.repository.plugins;

import kg.apc.emulators.TestJMeterUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.jmeterplugins.repository.Plugin;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.PluginManagerTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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

        URL testPlan = PluginManagerTest.class.getResource("/testplan.xml");
        Set<Plugin> plugins = suggester.findPluginsToInstall("Loading file : " + testPlan.getPath());
        assertEquals(1, plugins.size());
        assertEquals("jpgc-plugin2", plugins.toArray(new Plugin[1])[0].getID());

        Set<String> classes = new HashSet<>();
        classes.add("kg.apc.jmeter.samplers.DummySamplerGui");
        plugins = suggester.findPluginsFromClasses(classes);
        assertEquals(1, plugins.size());
        assertEquals("jpgc-plugin2", plugins.toArray(new Plugin[1])[0].getID());

        pmgr.togglePlugins(pmgr.getAvailablePlugins(), true);
        String msg = pmgr.getChangesAsText();

        assertTrue(msg.contains("jpgc-plugin1"));
        assertTrue(msg.contains("jpgc-plugin2"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        JMeterUtils.getJMeterProperties().remove("jpgc.repo.address");
    }
}