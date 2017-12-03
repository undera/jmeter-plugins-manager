package org.jmeterplugins.repository.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.plugins.PluginSuggester;


@Plugin(name = "Logger", category = "Core", elementType = "appender", printObject = true)
public class LoggerAppender extends AbstractAppender {

    protected PluginSuggester suggester;

    public LoggerAppender(String name, PluginManager pmgr) {
        super(name, new SaveServiceFilter(Filter.Result.ACCEPT, Filter.Result.DENY), PatternLayout.createDefaultLayout());
        start();
        Configuration configuration = ((LoggerContext) LogManager.getContext(false)).getConfiguration();
        configuration.getRootLogger().addAppender(this, Level.INFO, new SaveServiceFilter(Filter.Result.ACCEPT, Filter.Result.DENY));
        this.suggester = new PluginSuggester(pmgr);
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

    public static class SaveServiceFilter extends AbstractFilter {
        public SaveServiceFilter(Result onMatch, Result onMismatch) {
            super(onMatch, onMismatch);
        }

        @Override
        public Result filter(LogEvent event) {
            return event.getLoggerName().contains("SaveService") ? onMatch : onMismatch;
        }
    }
}
