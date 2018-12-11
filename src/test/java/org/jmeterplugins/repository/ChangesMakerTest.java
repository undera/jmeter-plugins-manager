package org.jmeterplugins.repository;


import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class ChangesMakerTest {
    @Test
    public void getProcessBuilder() throws Exception {
        Assert.assertEquals("a b", URLDecoder.decode("a%20b", "UTF-8"));
    }

    @Test
    public void getRestartFile() throws Exception {
        Map<Plugin, Boolean> map = new HashMap<>();
        ChangesMaker maker = new ChangesMaker(map);

        LinkedList<String> options = new LinkedList<>();
        options.add("-t");
        options.add("testPlanFile");

        File file = maker.getRestartFile(options);
        List<String> lines = Files.readAllLines(Paths.get(file.toURI()), Charset.defaultCharset());
        String fileContent = Arrays.toString(lines.toArray());

        assertTrue(fileContent, fileContent.contains("ApacheJMeter.jar, -t, testPlanFile"));
    }

    @Test
    public void getInstallFile() throws Exception {
        Map<Plugin, Boolean> map = new HashMap<>();
        ChangesMaker obj = new ChangesMaker(map);
        Set<Plugin> plugins = new HashSet<>();
        PluginMock p1 = new PluginMock("test", "test");
        p1.setInstallerClass("test1");
        p1.setDestName("plugin1");
        Map<String, String> libs1 = new HashMap<>();
        libs1.put("lib_to_install1", "path...");
        p1.setLibs(libs1);

        PluginMock p2 = new PluginMock("test", "test");
        p2.setInstallerClass("test2");
        p2.setDestName("plugin2");
        Map<String, String> libs2 = new HashMap<>();
        libs2.put("jmeter-plugins-emulators", "path...");
        p2.setLibs(libs2);

        plugins.add(p1);
        plugins.add(p2);

        HashSet<Library.InstallationInfo> installationInfos = new HashSet<>();
        installationInfos.add(new Library.InstallationInfo("lib_to_install1", "tmp_path", "some_path_to_jar1"));
        File res = obj.getInstallFile(plugins, installationInfos);

        assertTrue(res.length() > 0);

        String installFirst = "some_path_to_jar1" + File.pathSeparator + "plugin1\ttest1";
        String installSecond = "jmeter-plugins-emulators-0.2.jar" + File.pathSeparator + "plugin2\ttest2";

        List<String> lines = Files.readAllLines(Paths.get(res.toURI()), Charset.defaultCharset());
        String fileContent = Arrays.toString(lines.toArray());
        System.out.println(fileContent);

        assertTrue(fileContent, fileContent.contains(installFirst));
        assertTrue(fileContent, fileContent.contains(installSecond));

        File res2 = obj.getMovementsFile(plugins, plugins, new HashSet<Library.InstallationInfo>(), new HashSet<String>());
        assertTrue(res2.length() > 0);
    }

    @Test
    public void getMovementsFile() throws Exception {
        Map<Plugin, Boolean> map = new HashMap<>();
        ChangesMaker obj = new ChangesMaker(map);
        File file = File.createTempFile("tmp", "");
        file.deleteOnExit();
        obj.getProcessBuilder(file, file, file);
    }

}