package org.jmeterplugins.repository.logging;

import org.apache.log.LogEvent;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.plugins.PluginSuggester;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoggerPanelWrappingTest {

    @Test
    public void testFlow() throws Exception {
        LoggerPanelWrapping wrapping = new LoggerPanelWrapping(PluginManager.getStaticManager());
        PluginSuggester suggester = new PluginSuggester(PluginManager.getStaticManager());
        wrapping.setSuggester(suggester);
        assertEquals(suggester, wrapping.getSuggester());
        LogEvent event = new LogEvent();
        event.setCategory("SaveService");
        event.setMessage("Save File");
        wrapping.processEvent(event);
    }
}