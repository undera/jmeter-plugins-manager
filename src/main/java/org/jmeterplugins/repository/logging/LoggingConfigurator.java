package org.jmeterplugins.repository.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

/**
 *  Configure log4j logging for JMeter since 3.2 in PluginsManagerCMD
 */
public class LoggingConfigurator {


    public static void main(String[] args) {
        new LoggingConfigurator();

    }

    public LoggingConfigurator() {
        configure();
    }

    public void configure() {
        PatternLayout.Builder patternBuilder = PatternLayout.newBuilder();
        patternBuilder.withPattern("%d %p %c{1.}: %m%n");
        PatternLayout layout = patternBuilder.build();

        ConsoleAppender consoleAppender = ConsoleAppender.createDefaultAppenderForLayout(layout);
        consoleAppender.start();

        FileAppender.Builder fileAppenderBuilder = new FileAppender.Builder();
        fileAppenderBuilder.withName("file-appender");
        fileAppenderBuilder.withLayout(layout);
        fileAppenderBuilder.withFileName("PluginManagerCMD.log");
        fileAppenderBuilder.withAppend(false);

        FileAppender fileAppender = fileAppenderBuilder.build();
        fileAppender.start();

        Configuration configuration = ((LoggerContext) LogManager.getContext(false)).getConfiguration();

        LoggerConfig rootLogger = configuration.getRootLogger();
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(consoleAppender, Level.INFO, null);
        rootLogger.addAppender(fileAppender, Level.INFO, null);
    }

    @Plugin(name = "Logger", category = "Core", elementType = "appender", printObject = true)
    public static class SimpleConsoleAppender extends AbstractAppender {

        protected SimpleConsoleAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
            super(name, filter, layout);
        }

        @Override
        public void append(LogEvent event) {
            System.out.println(getName() + " : " + new String(getLayout().toByteArray(event)));
        }
    }
}
