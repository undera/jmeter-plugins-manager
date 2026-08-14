package org.jmeterplugins.repository.logging;

import org.jmeterplugins.repository.PluginManager;
import org.slf4j.LoggerFactory;

public class LoggingHooker {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoggingHooker.class);
    private final PluginManager mgr;

    public LoggingHooker(PluginManager mgr) {
        this.mgr = mgr;
    }

    public void hook() {
        try {
            new LoggerAppender("pmgr-logging-appender", mgr);
        } catch (Throwable ex) {
            log.error("Cannot hook into logging", ex);
        }
    }
}
