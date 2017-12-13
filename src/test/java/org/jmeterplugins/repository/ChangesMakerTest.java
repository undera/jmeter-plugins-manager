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
        PluginMock p = new PluginMock("test", "test");
        p.setInstallerClass("test");
        plugins.add(p);
        File res = obj.getInstallFile(plugins);
        assertTrue(res.length() > 0);

        File res2 = obj.getMovementsFile(plugins, plugins, new HashMap<String, String>(), new HashSet<String>());
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