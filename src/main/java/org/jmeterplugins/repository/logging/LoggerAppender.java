package org.jmeterplugins.repository.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jmeterplugins.repository.plugins.PluginSuggester;

@Plugin(name = "Logger", category = "Core", elementType = "appender", printObject = true)
public class LoggerAppender extends AbstractAppender {

    protected PluginSuggester suggester;


    public LoggerAppender(String name) {
        super(name, null, PatternLayout.createDefaultLayout());
        org.apache.logging.log4j.core.config.Configuration configuration =
                ((org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false)).getConfiguration();
        configuration.getRootLogger().addAppender(
                this,
                org.apache.logging.log4j.Level.INFO, null);
        this.suggester = new PluginSuggester();
    }

    @Override
    public void append(LogEvent logEvent) {
        if (logEvent.getLoggerName().contains("SaveService")) {
            suggester.checkAndSuggest(logEvent.getMessage().getFormattedMessage());
        }
    }

    public PluginSuggester getSuggester() {
        return suggester;
    }

    public void setSuggester(PluginSuggester suggester) {
        this.suggester = suggester;
    }
}
