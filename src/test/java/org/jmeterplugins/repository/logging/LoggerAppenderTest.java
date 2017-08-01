package org.jmeterplugins.repository.logging;


import kg.apc.emulators.TestJMeterUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.MessageFormatMessage;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.PluginManagerTest;
import org.jmeterplugins.repository.plugins.PluginSuggester;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class LoggerAppenderTest {

    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
        URL url = PluginManagerTest.class.getResource("/testVirtualPlugin.json");
        JMeterUtils.setProperty("jpgc.repo.address", url.getFile());
    }

    @Test
    public void testFlow() throws Exception {
        PluginManager pmgr = new PluginManager();
        LoggerAppender appender = new LoggerAppender("test-appender", pmgr);
        PluginSuggester suggester = new PluginSuggester(pmgr);
        appender.setSuggester(suggester);
        assertEquals(suggester, appender.getSuggester());

        Log4jLogEvent.Builder builder = Log4jLogEvent.newBuilder();
        builder.setMessage(new MessageFormatMessage("Save file"));
        builder.setLoggerName("SaveService");
        appender.append(builder.build());

        LoggerAppender.SaveServiceFilter filter = new LoggerAppender.SaveServiceFilter(Filter.Result.ACCEPT, Filter.Result.DENY);
        assertEquals(Filter.Result.ACCEPT, filter.filter(builder.build()));
        builder.setLoggerName("SomeLogger");
        assertEquals(Filter.Result.DENY, filter.filter(builder.build()));
    }
}