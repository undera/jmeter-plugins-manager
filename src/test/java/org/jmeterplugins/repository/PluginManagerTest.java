package org.jmeterplugins.repository;

import kg.apc.emulators.TestJMeterUtils;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.util.JMeterUtils;
import org.jmeterplugins.repository.exception.DownloadException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

import static org.junit.Assert.*;

public class PluginManagerTest {

    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
        URL url = PluginManagerTest.class.getResource("/testVirtualPlugin.json");
        JMeterUtils.setProperty("jpgc.repo.address", url.getFile());
        //reset static pmgr
        try {
            Field staticManager = PluginManager.class.getDeclaredField("staticManager");
            staticManager.setAccessible(true);
            staticManager.set(null, new PluginManager());
        } catch (Throwable ex) {
            fail("Failed to reset static pmgr");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        JMeterUtils.getJMeterProperties().remove("jpgc.repo.address");
    }

    @Test
    public void testResolve() throws IOException {
        Plugin[] init = new Plugin[]{};
        PluginManager obj = new PluginManagerEmul(init);
        obj.getChangesAsText();
    }

    @Test
    public void testStandardSet() throws Throwable {
        PluginManager pmgr = new PluginManager();
        pmgr.load();

        for (Plugin plugin : pmgr.allPlugins.keySet()) {
            if (plugin.getID().equals("jpgc-standard")) {
                assertTrue(pmgr.allPlugins.get(plugin));
            }
        }
    }

    /**
     * If Failed check, with what repo initialized STATIC pmgr.... it must be "/testVirtualPlugin.json"
     */
    @Test
    public void testStatus() throws IOException {
        String res = PluginManager.getAllPluginsStatus();
        String expected = "[jpgc-dep1=0.0.0-STOCK, jpgc-dep2=0.0.0-STOCK, jpgc-standard=2.0]";
        assertEquals(expected, res);
    }

    /**
     * If Failed check, with what repo initialized STATIC pmgr.... it must be "/testVirtualPlugin.json"
     */
    @Test
    public void testStatusSingle() throws IOException {
        assertEquals("0.0.0-STOCK", PluginManager.getPluginStatus("jpgc-dep2"));
        assertEquals(null, PluginManager.getPluginStatus("jmeter-nonexistent"));
    }

    @Test
    public void testReadOnly() throws Throwable {
        PluginManager mgr = new PluginManager();
        String jarPath = Plugin.getJARPath(JMeterEngine.class.getCanonicalName());
        assert jarPath != null;
        File ifile = new File(jarPath).getParentFile();
        ifile.setReadOnly();
        mgr.load();
        try {
            mgr.applyChanges(new LoggingCallback(), true, null);
            fail();
        } catch (RuntimeException e) {
            String prefix = "Have no write access for JMeter directories, not possible to use Plugins Manager:";
            assertTrue(e.getMessage().contains(prefix));
        } finally {
            ifile.setWritable(true);
        }
    }

    @Test
    public void testApplyChanges() throws Exception {
        String imgPath = "file:///" + new File(".").getAbsolutePath() + "/target/classes/org/jmeterplugins/logo.png";
        String str = "{\"id\": 0,  \"markerClass\": \"" + PluginsListTest.class.getName() + "\"," +
                " \"screenshotUrl\": \"" + imgPath + "\", \"name\": 3, \"description\": 4, \"helpUrl\": 5, \"vendor\": 5, \"installerClass\": \"test\", " +
                "\"versions\" : { \"0.1\" : { \"changes\": \"fix verified exception1\" }," +
                "\"0.2\" : { \"changes\": \"fix verified exception1\", \"libs\": {\n" +
                "          \"jpgc-common\": \"http://httpstat.us/500\"\n" +
                "        }}," +
                "\"0.3\" : { \"changes\": \"fix verified exception1\", \"downloadUrl\": \"http://httpstat.us/500\" } }}";

        String addr = JMeterUtils.getPropDefault("jpgc.repo.address", "https://jmeter-plugins.org/repo/");
        try {
            JMeterUtils.setProperty("jpgc.repo.address", "http://httpstat.us/500");

            Plugin p = Plugin.fromJSON(JSONObject.fromObject(str, new JsonConfig()));
            PluginManager manager = new PluginManager();
            manager.allPlugins.put(p, true); // need to install
            p.setCandidateVersion("9999");

            try {
                manager.applyChanges(new GenericCallback<String>() {
                    @Override
                    public void notify(String progress) {
                    }
                }, true, null);
                fail();
            } catch (IllegalArgumentException ex) {
                assertTrue(ex.getMessage().contains("Version 9999 not found for plugin"));
            }

            manager.allPlugins.put(p, true); // need to install
            p.setCandidateVersion("0.2");

            try {
                manager.applyChanges(new GenericCallback<String>() {
                    @Override
                    public void notify(String progress) {
                    }
                }, true, null);
                fail();
            } catch (DownloadException ex) {
                assertTrue(ex.getMessage().contains("Failed to download library"));
            }

            manager = new PluginManager();
            manager.allPlugins.put(p, true); // need to install
            p.setCandidateVersion("0.3");
            try {
                manager.applyChanges(new GenericCallback<String>() {
                    @Override
                    public void notify(String progress) {
                    }
                }, true, null);
                fail();
            } catch (DownloadException ex) {
                assertTrue(ex.getMessage().contains("Failed to download plugin"));
            }

        } finally {
            JMeterUtils.setProperty("jpgc.repo.address", addr);
        }
    }

    @Test
    public void testRetryDownload() throws Throwable {
        String addr = JMeterUtils.getPropDefault("jpgc.repo.address", "https://jmeter-plugins.org/repo/");
        JMeterUtils.setProperty("jpgc.repo.address", "http://httpstat.us/500");
        PluginManager mgr = new PluginManager();
        long start = System.currentTimeMillis();
        try {
            mgr.load();
            fail();
        } catch (IOException e) {
            assertEquals("Repository responded with wrong status code: 500", e.getMessage());

        } finally {
            JMeterUtils.setProperty("jpgc.repo.address", addr);
        }
        assertTrue(5000 < (System.currentTimeMillis() - start));
    }

    private class PluginManagerEmul extends PluginManager {
        public PluginManagerEmul(Plugin[] plugins) {
            for (Plugin p : plugins) {
                allPlugins.put(p, p.isInstalled());
            }
        }

        @Override
        public String[] getUsageStats() {
            return super.getUsageStats();
        }
    }

    private class LoggingCallback implements GenericCallback<String> {
        @Override
        public void notify(String s) {
            System.out.println(s);
        }
    }

    @Test
    public void testRemoveVerisonFromJAR() throws Exception {
        assertEquals("Apache_core", PluginManager.removeJARVersion("Apache_core-5.2.5"));
        assertEquals("jmeter1.1api", PluginManager.removeJARVersion("jmeter-1.1-api-2.0.0"));
        assertEquals("junit", PluginManager.removeJARVersion("junit"));
        assertEquals("java", PluginManager.removeJARVersion("java-1.7"));
        assertEquals("testapi", PluginManager.removeJARVersion("test-api"));
        assertEquals("commonsjexl", PluginManager.removeJARVersion("commons-jexl-1.1"));
    }

    @Test
    public void testBuildMap() throws Throwable {
        PluginManager mgr=new PluginManager();
        mgr.load();
        mgr.logPluginComponents();
    }
}