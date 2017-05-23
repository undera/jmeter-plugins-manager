package org.jmeterplugins.repository;

import kg.apc.emulators.TestJMeterUtils;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import org.apache.jmeter.util.JMeterUtils;
import org.jmeterplugins.repository.exception.DownloadException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.*;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class PluginManagerDialogTest {
    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
    }

    @Test
    public void displayGUI() throws Throwable {
        if (!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
//            URL url = PluginManagerTest.class.getResource("/lib_versions.json");
//            JMeterUtils.setProperty("jpgc.repo.address", url.getPath());
            System.setProperty("http.proxyHost", "localhost");
            System.setProperty("http.proxyPort", "81");
            PluginManager aManager = new PluginManager();
            aManager.load();
            PluginManagerDialog frame = new PluginManagerDialog(aManager);

            frame.setPreferredSize(new Dimension(800, 600));
            frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
            while (frame.isVisible()) {
                Thread.sleep(1000);
            }
            JMeterUtils.getJMeterProperties().remove("jpgc.repo.address");
        }
    }



    @Test
    public void testFailDownload() throws Exception {
        if (!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            String addr = JMeterUtils.getPropDefault("jpgc.repo.address", "https://jmeter-plugins.org/repo/");
            try {
                JMeterUtils.setProperty("jpgc.repo.address", "http://httpstat.us/500");
                PluginManager aManager = new PluginManager();
                PluginManagerDialog frame = new PluginManagerDialog(aManager);
                frame.componentShown(null);
                List<JEditorPane> panes = new ArrayList<>();
                getJEditorPane(frame, panes);
                assertTrue(panes.size() > 0);
                boolean isOk = false;
                for (JEditorPane p : panes) {
                    if (p.getText().contains("Failed to download plugins repository.")) {
                        isOk = true;
                        break;
                    }
                }
                assertTrue(isOk);
                frame.actionPerformed(null);
            } finally {
                JMeterUtils.setProperty("jpgc.repo.address", addr);
            }
        }
    }

    private void getJEditorPane(Container component, List<JEditorPane> result) {
        Component[] components = component.getComponents();
        for (Component component1 : components) {

            if (component1 instanceof JEditorPane) {
                result.add((JEditorPane) component1);
            }

            if (component1 instanceof Container) {
                getJEditorPane((Container) component1, result);
            }
        }
    }
}