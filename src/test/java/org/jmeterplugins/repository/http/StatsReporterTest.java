package org.jmeterplugins.repository.http;

import kg.apc.emulators.TestJMeterUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.jmeterplugins.repository.JARSourceEmul;
import org.jmeterplugins.repository.JARSourceFilesystem;
import org.jmeterplugins.repository.JARSourceHTTP;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.*;

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
            public void reportStats(String[] usageStats) {
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
        assertNotSame(jarSource, reporter.getJarSource());

        assertTrue(reporter.getJarSource() instanceof JARSourceHTTPExt);
        JARSourceHTTPExt actual = (JARSourceHTTPExt) reporter.getJarSource();
        assertNotSame(jarSource.getHttpClient(), actual.getHttpClient());

    }

    public static class JARSourceHTTPExt extends JARSourceHTTP {
        public JARSourceHTTPExt(String jmProp) {
            super(jmProp);
        }

        public AbstractHttpClient getHttpClient() {
            return httpClient;
        }

        @Override
        public HttpResponse execute(HttpUriRequest request, HttpContext context) {
            ProtocolVersion http = new ProtocolVersion("HTTP", 1, 1);
            BasicStatusLine accepted = new BasicStatusLine(http, 200, "OK");
            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(accepted);
            basicHttpResponse.addHeader("Last-Modified", JARSourceHTTP.dateFormat.format(System.currentTimeMillis() - 1000));
            BasicHttpEntity entity = new BasicHttpEntity();
            InputStream is=new ByteArrayInputStream("[]".getBytes());
            entity.setContent(is);
            basicHttpResponse.setEntity(entity);
            return basicHttpResponse;
        }
    }

    @Test
    public void testCloneable1() throws Exception {
        JARSourceFilesystem filesystem = new JARSourceFilesystem(new File(""));
        final String[] stats = {"aaaa"};
        StatsReporter reporter = new StatsReporter(filesystem, stats);
        assertNotNull(reporter.getJarSource());
        assertNotSame(filesystem, reporter.getJarSource());
    }

    @Test
    public void testFlow3() throws Exception {
        JARSourceHTTPExt jarSource = new JARSourceHTTPExt("repoStats");
        jarSource.getRepo();
        Thread.sleep(500);
        final String[] stats = {"aaaa"};
        jarSource.reportStats(stats);
    }
}