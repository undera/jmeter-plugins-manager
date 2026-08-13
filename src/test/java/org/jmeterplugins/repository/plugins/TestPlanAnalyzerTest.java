package org.jmeterplugins.repository.plugins;

import org.jmeterplugins.repository.JMeterTestEnv;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;


public class TestPlanAnalyzerTest {

    @BeforeClass
    public static void setup() {
        JMeterTestEnv.createJMeterEnv();
    }

    /**
     * Present on the classpath, but cannot be initialised - the case that used to be reported
     * as "class not found", which made the suggester offer plugins the user already had.
     */
    public static class FailsToInitialise {
        static {
            if (true) {
                throw new IllegalStateException("boom");
            }
        }
    }

    @Test
    public void testExistingClassIsNotReportedMissing() {
        assertTrue(TestPlanAnalyzer.isClassExists("org.apache.jmeter.save.SaveService"));
    }

    @Test
    public void testAbsentClassIsReportedMissing() {
        assertFalse(TestPlanAnalyzer.isClassExists("no.such.plugin.Component"));
    }

    @Test
    public void testUnloadableClassIsNotReportedMissing() {
        String name = FailsToInitialise.class.getName();
        // first touch raises ExceptionInInitializerError, later ones NoClassDefFoundError;
        // neither means the class is absent, so neither may be reported as missing
        assertTrue(TestPlanAnalyzer.isClassExists(name));
        assertTrue(TestPlanAnalyzer.isClassExists(name));
    }

    @Test
    public void test() throws Exception {
        String path = getClass().getResource("/testplan.xml").getPath();
        TestPlanAnalyzer analyzer = new TestPlanAnalyzer();
        Set<String> classes = analyzer.analyze(path);

        assertEquals(4, classes.size());
        assertTrue(classes.contains("kg.apc.jmeter.vizualizers.ResponseTimesOverTimeGui"));
        assertTrue(classes.contains("kg.apc.jmeter.samplers.DummySampler"));
        assertTrue(classes.contains("kg.apc.jmeter.vizualizers.CorrectedResultCollector"));
        assertTrue(classes.contains("kg.apc.jmeter.samplers.DummySamplerGui"));
    }

    @Test
    public void testBackendListenerImpl() throws Exception {
        String path = getClass().getResource("/testplan-with-backend-listener.xml").getPath();
        TestPlanAnalyzer analyzer = new TestPlanAnalyzer();
        Set<String> classes = analyzer.analyze(path);

        assertEquals(5, classes.size());
        assertTrue(classes.contains("kg.apc.jmeter.vizualizers.ResponseTimesOverTimeGui"));
        assertTrue(classes.contains("kg.apc.jmeter.samplers.DummySampler"));
        assertTrue(classes.contains("kg.apc.jmeter.vizualizers.CorrectedResultCollector"));
        assertTrue(classes.contains("kg.apc.jmeter.samplers.DummySamplerGui"));
        assertTrue(classes.contains("io.github.adrianmo.jmeter.backendlistener.azure.AzureBackendClient"));
    }
}