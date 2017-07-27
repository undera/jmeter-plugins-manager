package org.jmeterplugins.repository.logging;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jmeterplugins.repository.plugins.TestPlanAnalyzer;

import java.io.Serializable;

@Plugin(name = "Logger", category = "Core", elementType = "appender", printObject = true)
public class LoggerAppender extends AbstractAppender {

    protected TestPlanAnalyzer analyzer;


    public LoggerAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
        this.analyzer = new TestPlanAnalyzer();
    }

    public LoggerAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        this.analyzer = new TestPlanAnalyzer();
    }

    @Override
    public void append(LogEvent logEvent) {
        System.out.println(this + " " + logEvent.getMessage().getFormattedMessage());
    }

    @PluginFactory
    public static LoggerAppender createAppender(@PluginAttribute("name") String name,
                                                     @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                     @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filters") Filter filter) {

        if (name == null) {
            LOGGER.error("No name provided for LoggerAppender");
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        return new LoggerAppender(name, filter, layout, ignoreExceptions);
    }

    public TestPlanAnalyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(TestPlanAnalyzer analyzer) {
        this.analyzer = analyzer;
    }
}
