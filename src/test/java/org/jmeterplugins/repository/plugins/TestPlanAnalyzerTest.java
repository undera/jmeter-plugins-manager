package org.jmeterplugins.repository.plugins;

import kg.apc.emulators.TestJMeterUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;


public class TestPlanAnalyzerTest {

    @BeforeClass
    public static void setup() {
        TestJMeterUtils.createJmeterEnv();
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