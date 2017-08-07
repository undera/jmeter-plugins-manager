package org.jmeterplugins.repository;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DependencyResolverTest {
    @Test
    public void testSimpleInstall() throws Exception {
        Map<Plugin, Boolean> plugs = new HashMap<>();
        PluginMock install = new PluginMock("install", null);
        Map<String, String> libs = new HashMap<>();
        libs.put("test", "test");
        libs.put("jorphan", "test");
        install.setLibs(libs);
        plugs.put(install, true);
        PluginMock uninstall = new PluginMock("uninstall", "1.0");
        plugs.put(uninstall, false);

        DependencyResolver obj = new DependencyResolver(plugs);
        Set<Plugin> adds = obj.getAdditions();
        Set<Plugin> dels = obj.getDeletions();
        Map<String, String> libAdds = obj.getLibAdditions();

        assertEquals(1, adds.size());
        assertEquals(1, dels.size());
        assertEquals(1, libAdds.size());
        assertTrue(adds.contains(install));
        assertTrue(dels.contains(uninstall));
    }

    @Test
    public void testUpgrade() throws Exception {
        Map<Plugin, Boolean> plugs = new HashMap<>();
        PluginMock upgrade = new PluginMock("install", "1.0");
        upgrade.setCandidateVersion("0.1");
        plugs.put(upgrade, true);

        DependencyResolver obj = new DependencyResolver(plugs);
        Set<Plugin> adds = obj.getAdditions();
        Set<Plugin> dels = obj.getDeletions();

        assertEquals(1, adds.size());
        assertEquals(1, dels.size());
        assertTrue(adds.contains(upgrade));
        assertTrue(dels.contains(upgrade));
    }

    @Test
    public void testDepInstall() throws Exception {
        Map<Plugin, Boolean> plugs = new HashMap<>();
        PluginMock jdbc = new PluginMock("jdbc", null);
        plugs.put(jdbc, false);
        PluginMock http = new PluginMock("http", null);
        plugs.put(http, false);
        PluginMock components = new PluginMock("components", null);
        plugs.put(components, false);

        PluginMock standard = new PluginMock("standard", null);
        HashSet<String> depsStandard = new HashSet<>();
        depsStandard.add(http.getID());
        depsStandard.add(components.getID());
        standard.setDepends(depsStandard);
        plugs.put(standard, false);

        PluginMock extras = new PluginMock("extras", null);
        HashSet<String> depsExtras = new HashSet<>();
        depsExtras.add(standard.getID());
        depsExtras.add(jdbc.getID());
        depsExtras.add(http.getID());
        extras.setDepends(depsExtras);
        plugs.put(extras, true);

        DependencyResolver obj = new DependencyResolver(plugs);
        Set<Plugin> adds = obj.getAdditions();
        Set<Plugin> dels = obj.getDeletions();

        assertEquals(5, adds.size());
        assertEquals(0, dels.size());
        assertTrue(adds.contains(jdbc));
        assertTrue(adds.contains(components));
        assertTrue(adds.contains(standard));
        assertTrue(adds.contains(extras));
        assertTrue(adds.contains(http));
    }

    @Test
    public void testDepInstallJMeterHTTP() throws Exception {
        Map<Plugin, Boolean> plugs = new HashMap<>();

        PluginMock cause = new PluginMock("cause", Plugin.getJMeterVersion());
        cause.setVersions(JSONObject.fromObject("{\"\":null}", new JsonConfig()));
        plugs.put(cause, true);

        PluginMock effect = new PluginMock("effect", null);
        HashSet<String> deps = new HashSet<>();
        deps.add(cause.getID());
        effect.setDepends(deps);
        plugs.put(effect, true);

        DependencyResolver obj = new DependencyResolver(plugs);
        Set<Plugin> adds = obj.getAdditions();
        Set<Plugin> dels = obj.getDeletions();

        assertTrue(adds.contains(effect));
        assertFalse(adds.contains(cause));
        assertEquals(1, adds.size());
        assertEquals(0, dels.size());
    }

    @Test
    public void testDepUninstall() throws Exception {
        Map<Plugin, Boolean> plugs = new HashMap<>();
        PluginMock a = new PluginMock("root", "1.0");
        plugs.put(a, false);

        PluginMock b = new PluginMock("cause", "1.0");
        HashSet<String> deps = new HashSet<>();
        deps.add(a.getID());
        b.setDepends(deps);
        plugs.put(b, true);

        DependencyResolver obj = new DependencyResolver(plugs);
        Set<Plugin> adds = obj.getAdditions();
        Set<Plugin> dels = obj.getDeletions();

        assertEquals(0, adds.size());
        assertEquals(2, dels.size());
        assertTrue(dels.contains(a));
        assertTrue(dels.contains(b));
    }

    @Test
    public void testDepLibUninstall() throws Exception {
        Map<Plugin, Boolean> plugs = new HashMap<>();

        PluginMock a = new PluginMock("a", "1.0");
        Map<String, String> aLibs = new HashMap<>();
        aLibs.put("aaa", "");
        aLibs.put("guava", "");
        aLibs.put("bbb", "");
        a.setLibs(aLibs);
        plugs.put(a, false);

        PluginMock b = new PluginMock("b", null);
        Map<String, String> bLibs = new HashMap<>();
        bLibs.put("aa", "");
        bLibs.put("guava", "");
        bLibs.put("bb", "");
        b.setLibs(bLibs);
        plugs.put(b, true);

        DependencyResolver obj = new DependencyResolver(plugs);
        Set<Plugin> adds = obj.getAdditions();
        Set<Plugin> dels = obj.getDeletions();

        assertTrue(!obj.getLibDeletions().contains("guava"));
        assertEquals(1, adds.size());
        assertEquals(1, dels.size());
        assertTrue(dels.contains(a));
        assertTrue(adds.contains(b));
    }

    @Test
    public void testLibVersionManagement() throws Exception {
        URL url = PluginManagerTest.class.getResource("/lib_versions.json");
        JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(FileUtils.readFileToString(new File(url.getPath())), new JsonConfig());

        Map<Plugin, Boolean> map = new HashMap<>();
        for (Object obj : jsonArray) {
            Plugin plugin = Plugin.fromJSON((JSONObject) obj);
            plugin.detectInstalled(new HashSet<Plugin>());
            map.put(plugin, true);
        }


        DependencyResolver resolver = new DependencyResolver(map);

        Map<String, String> libs = resolver.getLibAdditions();

        for (String libName : libs.keySet()) {
            if (libName.equals("jmeter-plugins-cmn-jmeter")) {
                assertEquals("jmeter-plugins-cmn-jmeter-0.4.jar", libs.get(libName));
            } else if (libName.equals("kafka_2.8.2")) {
                assertEquals("kafka_2.8.2_v0.8.jar", libs.get(libName));
            } else if (libName.equals("commons-io")) {
                assertEquals("commons-io.jar", libs.get(libName));
            } else if (libName.equals("kafka_2.1.0")) {
                assertEquals("kafka_2.1.0_v.0.1.jar", libs.get(libName));
            } else if (libName.equals("lib")) {
                assertEquals("lib-0.8.jar", libs.get(libName));
            } else if (libName.equals("lib2")) {
                assertEquals("lib2-0.2.jar", libs.get(libName));
            } else {
                fail("Unexpected lib name: " + libName);
            }

        }
    }

    @Test
    public void testResolveLibBeforeDetete() throws Exception {
        URL url = PluginManagerTest.class.getResource("/lib_for_delete");
        JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(FileUtils.readFileToString(new File(url.getPath())), new JsonConfig());

        Map<Plugin, Boolean> map = new HashMap<>();
        for (Object obj : jsonArray) {
            Plugin plugin = Plugin.fromJSON((JSONObject) obj);
            plugin.detectInstalled(new HashSet<Plugin>());
            plugin.installedPath = "";
            plugin.installedVersion = "0.1";

            map.put(plugin, !plugin.getName().startsWith("delete"));
        }


        DependencyResolver resolver = new DependencyResolver(map);

        Set<String> libs = resolver.getLibDeletions();

        assertEquals(3, libs.size());
        assertTrue(libs.contains("ApacheJMeter_core"));
        assertTrue(libs.contains("commons-lang3"));
        assertTrue(libs.contains("commons-httpclient"));
    }

    @Test
    public void testResolveDowngradeWithNPE() throws Exception {
        URL url = PluginManagerTest.class.getResource("/self_npe.json");
        JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(FileUtils.readFileToString(new File(url.getPath())), new JsonConfig());

        Map<Plugin, Boolean> map = new HashMap<>();
        for (Object obj : jsonArray) {
            Plugin plugin = Plugin.fromJSON((JSONObject) obj);
            plugin.detectInstalled(new HashSet<Plugin>());
            plugin.installedPath = "";
            plugin.installedVersion = "0.14";
            plugin.candidateVersion = "0.13";

            map.put(plugin, true);
        }

        DependencyResolver resolver = new DependencyResolver(map);

        Map<String, String> libs = resolver.getLibAdditions();

        assertEquals(1, libs.size());
        assertNotNull(libs.get("cmdbeginner"));

        Set<Plugin> pluginsAdd = resolver.getAdditions();
        assertEquals(1, pluginsAdd.size());
        assertEquals("jpgc-plugins-manager", pluginsAdd.toArray(new Plugin[1])[0].getID());

        Set<Plugin> pluginsDelete = resolver.getDeletions();
        assertEquals(1, pluginsDelete.size());
        assertEquals("jpgc-plugins-manager", pluginsDelete.toArray(new Plugin[1])[0].getID());
    }

    @Test
    public void testResolveMissingLib() throws Exception {
        URL url = PluginManagerTest.class.getResource("/self_npe.json");
        JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(FileUtils.readFileToString(new File(url.getPath())), new JsonConfig());

        Map<Plugin, Boolean> map = new HashMap<>();
        for (Object obj : jsonArray) {
            Plugin plugin = Plugin.fromJSON((JSONObject) obj);
            plugin.detectInstalled(new HashSet<Plugin>());
            plugin.installedPath = "";
            plugin.installedVersion = "0.14";

            map.put(plugin, true);
        }

        DependencyResolver resolver = new DependencyResolver(map);

        Map<String, String> libs = resolver.getLibAdditions();

        assertEquals(1, libs.size());
        assertNotNull(libs.get("cmdbeginner"));

        Set<Plugin> pluginsAdd = resolver.getAdditions();
        assertEquals(1, pluginsAdd.size());
        assertEquals("jpgc-plugins-manager", pluginsAdd.toArray(new Plugin[1])[0].getID());

        Set<Plugin> pluginsDelete = resolver.getDeletions();
        assertEquals(1, pluginsDelete.size());
        assertEquals("jpgc-plugins-manager", pluginsDelete.toArray(new Plugin[1])[0].getID());
    }

    @Test
    public void testResolveLibIfLibBroken() throws Exception {
        URL url = PluginManagerTest.class.getResource("/broken.json");
        JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(FileUtils.readFileToString(new File(url.getPath())), new JsonConfig());

        Map<Plugin, Boolean> map = new HashMap<>();
        for (Object obj : jsonArray) {
            Plugin plugin = Plugin.fromJSON((JSONObject) obj);
            plugin.detectInstalled(new HashSet<Plugin>());
            plugin.installedPath = "";
            plugin.installedVersion = "0.1";

            map.put(plugin, true);
        }


        DependencyResolver resolver = new DependencyResolver(map);

        Set<String> libs = resolver.getLibDeletions();

        assertEquals(1, libs.size());
        assertTrue(libs.contains("bsf"));

        Map<String, String> libAdditions = resolver.getLibAdditions();
        assertEquals(1, libAdditions.size());
        assertEquals("lib-99.8.jar", libAdditions.get("bsf"));
    }

    @Test
    public void testUpdateLibWithPlugin() throws Exception {
        URL url = PluginManagerTest.class.getResource("/lib_update.json");
        JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(FileUtils.readFileToString(new File(url.getPath())), new JsonConfig());

        Map<Plugin, Boolean> map = new HashMap<>();
        for (Object obj : jsonArray) {
            Plugin plugin = Plugin.fromJSON((JSONObject) obj);
            plugin.detectInstalled(new HashSet<Plugin>());
            plugin.installedPath = "";
            plugin.installedVersion = "0.1";

            map.put(plugin, true);
        }


        DependencyResolver resolver = new DependencyResolver(map);

        Set<String> libsDeletions = resolver.getLibDeletions();

        assertEquals(2, libsDeletions.size());
        assertTrue(libsDeletions.contains("cmdrunner"));
        assertTrue(libsDeletions.contains("commons-codec"));

        Map<String, String> libsAdditions = resolver.getLibAdditions();
        assertEquals(2, libsAdditions.size());
        assertEquals("cmdrunner-9999.8.jar", libsAdditions.get("cmdrunner"));
        assertEquals("commons-codec-999.5.jar", libsAdditions.get("commons-codec"));
    }

    @Test
    public void testUpdateWhenInstall() throws Exception {
        URL url = PluginManagerTest.class.getResource("/installed.json");
        JSONArray jsonArray = (JSONArray) JSONSerializer.toJSON(FileUtils.readFileToString(new File(url.getPath())), new JsonConfig());

        Map<Plugin, Boolean> map = new HashMap<>();
        for (Object obj : jsonArray) {
            Plugin plugin = Plugin.fromJSON((JSONObject) obj);
            plugin.detectInstalled(new HashSet<Plugin>());

            map.put(plugin, true);
        }


        DependencyResolver resolver = new DependencyResolver(map);

        Set<String> libsDeletions = resolver.getLibDeletions();

        assertEquals(1, libsDeletions.size());
        assertTrue(libsDeletions.contains("commons-codec"));

        Map<String, String> libsAdditions = resolver.getLibAdditions();
        assertEquals(1, libsAdditions.size());
        assertEquals("commons-codec-99.99.jar", libsAdditions.get("commons-codec"));
    }
}