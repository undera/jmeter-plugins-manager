package org.jmeterplugins.repository.logging;

import kg.apc.emulators.TestJMeterUtils;
import org.apache.log.LogEvent;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.plugins.PluginSuggester;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class LoggerPanelWrappingTest {
    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
    }

    @Test
    public void testFlow() throws Exception {
        if (!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            PluginManager pmgr = new PluginManager();
            LoggerPanelWrapping wrapping = new LoggerPanelWrapping(pmgr);
            PluginSuggester suggester = new PluginSuggester(pmgr);
            wrapping.setSuggester(suggester);
            assertEquals(suggester, wrapping.getSuggester());
            LogEvent event = new LogEvent();
            event.setCategory("SaveService");
            event.setMessage("Save File");
            wrapping.processEvent(event);
        }
    }
}