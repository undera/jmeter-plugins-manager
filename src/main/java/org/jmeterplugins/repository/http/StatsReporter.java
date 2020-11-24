package org.jmeterplugins.repository.http;

import org.jmeterplugins.repository.JARSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StatsReporter extends Thread {
    private static final Logger log = LoggerFactory.getLogger(StatsReporter.class);

    private final JARSource jarSource;
    private final String[] usageStats;

    public StatsReporter(JARSource jarSource, String[] usageStats) throws CloneNotSupportedException {
        this.jarSource = (JARSource) jarSource.clone();
        this.usageStats = usageStats;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            jarSource.reportStats(usageStats);
            log.debug("Finished send repo stats");
        } catch (IOException e) {
            log.warn("Failed to send repo stats", e);
        }
    }

    protected JARSource getJarSource() {
        return jarSource;
    }
}
