package org.jmeterplugins.repository;

import kg.apc.emulators.TestJMeterUtils;
import org.apache.jmeter.util.JMeterUtils;

/**
 * Sets up the JMeter environment tests run in.
 * <p>
 * jmeter-plugins-emulators extracts its saveservice properties to &lt;home&gt;/ss.props but points
 * the <code>saveservice_properties</code> property at that <em>absolute</em> path, while JMeter
 * resolves the property against JMeter home - so SaveService looks for &lt;home&gt;&lt;home&gt;/ss.props,
 * fails to initialise, and every lookup made through it afterwards reports "class not found".
 * Re-point the property relative to home so SaveService can start.
 */
public class JMeterTestEnv {

    public static void createJMeterEnv() {
        TestJMeterUtils.createJmeterEnv();

        String home = JMeterUtils.getJMeterHome();
        String saveService = JMeterUtils.getPropDefault("saveservice_properties", "");
        if (saveService.startsWith(home)) {
            JMeterUtils.setProperty("saveservice_properties", saveService.substring(home.length()));
        }
    }
}
