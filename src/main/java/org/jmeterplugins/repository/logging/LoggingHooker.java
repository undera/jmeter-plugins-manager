package org.jmeterplugins.repository.logging;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.LogTarget;
import org.apache.log.Logger;

import java.lang.reflect.Constructor;

public class LoggingHooker {
    private static final Logger log = LoggingManager.getLoggerForClass();

    public LoggingHooker() {
        try {
            if (!isJMeter32orLater()) {
                Logger logger = LoggingManager.getLoggerFor("jmeter.save.SaveService");
                logger.setLogTargets(new LogTarget[]{new LoggerPanelWrapping()});
            } else {
                Class cls = Class.forName("org.jmeterplugins.repository.logging.LoggerAppender");
                Constructor constructor = cls.getConstructor(String.class);
                constructor.newInstance("pmgr-logging-appender");
            }
        } catch (Throwable ex) {
            log.error("Cannot hook into logging", ex);
        }
    }

    public boolean isJMeter32orLater() {
        try {
            Class<?> cls = this.getClass().getClassLoader().loadClass("org.apache.jmeter.gui.logging.GuiLogEventBus");
            if (cls != null) {
                return true;
            }
        } catch (ClassNotFoundException ex) {
            log.debug("Class 'org.apache.jmeter.gui.logging.GuiLogEventBus' not found", ex);
            return false;
        } catch (Throwable ex) {
            log.warn("Fail to detect JMeter version", ex);
        }
        return false;
    }
}
