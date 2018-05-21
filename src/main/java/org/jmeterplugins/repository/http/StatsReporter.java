package org.jmeterplugins.repository.http;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.JARSource;

import java.io.IOException;

public class StatsReporter extends Thread {
    private static final Logger log = LoggingManager.getLoggerForClass();

    private final JARSource jarSource;
    private final String[] usageStats;

    public StatsReporter(JARSource jarSource, String[] usageStats) {
        this.jarSource = jarSource;
        this.usageStats = usageStats;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            jarSource.reportStats(usageStats);
        } catch (IOException e) {
            log.warn("Failed to send repo stats", e);
        }
    }
}
