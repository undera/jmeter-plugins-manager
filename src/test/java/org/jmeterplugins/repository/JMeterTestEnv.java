package org.jmeterplugins.repository;

import kg.apc.emulators.TestJMeterUtils;

/**
 * Sets up the JMeter environment tests run in.
 * <p>
 * Nothing beyond the emulators is needed here, and it is worth recording why, because this
 * class previously worked around a problem that no longer exists. jmeter-plugins-emulators
 * points <code>saveservice_properties</code> at an absolute path. JMeter 2.13 resolved that
 * property by concatenating it onto JMeter home, so the absolute path had to be stripped back
 * to a relative one first. Since JMeter 3.x the property goes through
 * {@link org.apache.jmeter.util.JMeterUtils#findFile}, which takes the literal path when it
 * exists - so the absolute path the emulators set is used as-is, and stripping it breaks
 * SaveService instead of fixing it.
 * <p>
 * The saveservice.properties the emulators ship is 2.13-era, which is fine: SaveService
 * computes a checksum over the file rather than comparing it to an expected one, and the
 * resulting "Bad _version" line is a warning. If a test ever needs an alias introduced after
 * 2.13, ship that JMeter's saveservice.properties instead - it is not in ApacheJMeter_core,
 * only in JMeter's bin distribution.
 */
public class JMeterTestEnv {

    public static void createJMeterEnv() {
        TestJMeterUtils.createJmeterEnv();
    }
}
