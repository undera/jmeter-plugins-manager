package org.jmeterplugins.repository.http;

import kg.apc.emulators.TestJMeterUtils;
import org.apache.http.impl.client.AbstractHttpClient;
import org.jmeterplugins.repository.JARSourceEmul;
import org.jmeterplugins.repository.JARSourceFilesystem;
import org.jmeterplugins.repository.JARSourceHTTP;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StatsReporterTest {
    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
    }

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

    @Test
    public void testCloneable() throws Exception {
        JARSourceHTTPExt jarSource = new JARSourceHTTPExt("repo1;repo2");
        final String[] stats = {"aaaa"};
        StatsReporter reporter = new StatsReporter(jarSource, stats);
        assertNotNull(reporter.getJarSource());
        assertFalse(jarSource == reporter.getJarSource());

        assertTrue(reporter.getJarSource() instanceof JARSourceHTTPExt);
        JARSourceHTTPExt actual = (JARSourceHTTPExt) reporter.getJarSource();
        assertFalse(jarSource.getHttpClient() == actual.getHttpClient());

    }

    public static class JARSourceHTTPExt extends JARSourceHTTP {
        public JARSourceHTTPExt(String jmProp) {
            super(jmProp);
        }

        public AbstractHttpClient getHttpClient() {
            return httpClient;
        }
    }

    @Test
    public void testCloneable1() throws Exception {
        JARSourceFilesystem filesystem = new JARSourceFilesystem(new File(""));
        final String[] stats = {"aaaa"};
        StatsReporter reporter = new StatsReporter(filesystem, stats);
        assertNotNull(reporter.getJarSource());
        assertFalse(filesystem == reporter.getJarSource());
    }

}