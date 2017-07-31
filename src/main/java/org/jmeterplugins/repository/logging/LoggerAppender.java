package org.jmeterplugins.repository.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jmeterplugins.repository.plugins.PluginSuggester;
import org.apache.logging.log4j.Level;


@Plugin(name = "Logger", category = "Core", elementType = "appender", printObject = true)
public class LoggerAppender extends AbstractAppender {

    protected PluginSuggester suggester;


    public LoggerAppender(String name) {
        super(name, null, PatternLayout.createDefaultLayout());
        start();
        Configuration configuration = ((LoggerContext) LogManager.getContext(false)).getConfiguration();
        configuration.getRootLogger().addAppender(this, Level.INFO, null);
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
