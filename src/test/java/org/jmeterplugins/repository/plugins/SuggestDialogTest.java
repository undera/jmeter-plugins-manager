package org.jmeterplugins.repository.plugins;

import kg.apc.emulators.TestJMeterUtils;
import org.jmeterplugins.repository.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.*;

public class SuggestDialogTest {

    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
    }

    @Test
    public void testComponent() throws Exception {
        PluginManager pmgr = new PluginManager();
        SuggestDialog suggestDialog = new SuggestDialog(null, pmgr, pmgr.getAvailablePlugins());
        assertTrue(suggestDialog.generateMessage(pmgr.getInstalledPlugins()).contains("Your JMeter"));
    }
}