package org.jmeterplugins.repository.logging;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.LogTarget;
import org.apache.log.Logger;
import org.jmeterplugins.repository.PluginManager;

import java.lang.reflect.Constructor;

public class LoggingHooker {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private final PluginManager mgr;

    public LoggingHooker(PluginManager mgr) {
        this.mgr = mgr;
    }

    public void hook() {
        try {
            if (!isJMeter32orLater()) {
                Logger logger = LoggingManager.getLoggerFor("jmeter.save.SaveService");
                logger.setLogTargets(new LogTarget[]{new LoggerPanelWrapping(mgr)});
            } else {
                Class cls = Class.forName("org.jmeterplugins.repository.logging.LoggerAppender");
                Constructor constructor = cls.getConstructor(String.class, PluginManager.class);
                constructor.newInstance("pmgr-logging-appender", mgr);
            }
        } catch (Throwable ex) {
            log.error("Cannot hook into logging", ex);
        }
    }

    public static boolean isJMeter32orLater() {
        try {
            Class<?> cls = LoggingHooker.class.getClassLoader().loadClass("org.apache.jmeter.gui.logging.GuiLogEventBus");
            if (cls != null) {
                return true;
            }
        } catch (ClassNotFoundException ex) {
            log.debug("Class 'org.apache.jmeter.gui.logging.GuiLogEventBus' not found", ex);
        } catch (Throwable ex) {
            log.warn("Fail to detect JMeter version", ex);
        }
        return false;
    }
}
