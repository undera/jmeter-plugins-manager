package org.jmeterplugins.repository.logging;

import org.apache.jmeter.gui.LoggerPanel;
import org.apache.log.LogEvent;
import org.jmeterplugins.repository.plugins.PluginSuggester;

public class LoggerPanelWrapping extends LoggerPanel {

    protected PluginSuggester suggester;

    public LoggerPanelWrapping() {
        super();
        this.suggester = new PluginSuggester();
    }

    @Override
    public void processEvent(LogEvent logEvent) {
        if (logEvent.getCategory().contains("SaveService")) {
            suggester.checkAndSuggest(logEvent.getMessage());
        }
    }

    public PluginSuggester getSuggester() {
        return suggester;
    }

    public void setSuggester(PluginSuggester suggester) {
        this.suggester = suggester;
    }
}
