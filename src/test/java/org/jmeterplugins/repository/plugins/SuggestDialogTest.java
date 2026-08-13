package org.jmeterplugins.repository.plugins;

import org.jmeterplugins.repository.JMeterTestEnv;
import org.jmeterplugins.repository.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class SuggestDialogTest {

    @BeforeClass
    public static void setup() {
        JMeterTestEnv.createJMeterEnv();
    }

    @Test
    public void testComponent() throws Exception {
        if (!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            PluginManager pmgr = new PluginManager();
            SuggestDialog suggestDialog = new SuggestDialog(null, pmgr, pmgr.getAvailablePlugins(), "path");
            suggestDialog.setVisible(true);
        }
    }
}