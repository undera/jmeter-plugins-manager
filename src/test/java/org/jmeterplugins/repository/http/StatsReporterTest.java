package org.jmeterplugins.repository.http;

import org.jmeterplugins.repository.JARSourceEmul;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class StatsReporterTest {

    @Test
    public void testFlow() throws Exception {
        final String[] stats = {"aaaa"};
        JARSourceEmul emul = new JARSourceEmul() {
            @Override
            public void reportStats(String[] usageStats) throws IOException {
                assertArrayEquals(stats, usageStats);
            }
        };
        StatsReporter reporter = new StatsReporter(emul, stats);
        assertTrue(reporter.isDaemon());
        reporter.start();
        reporter.join();
    }
}