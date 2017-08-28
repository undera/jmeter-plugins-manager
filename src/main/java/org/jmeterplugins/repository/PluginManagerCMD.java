package org.jmeterplugins.repository;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.*;

import kg.apc.cmdtools.AbstractCMDTool;

import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.jmeterplugins.repository.plugins.PluginSuggester;

import static org.jmeterplugins.repository.logging.LoggingHooker.isJMeter32orLater;

public class PluginManagerCMD extends AbstractCMDTool implements GenericCallback<String> {
    private static final Logger log = LoggingManager.getLoggerForClass();

    public PluginManagerCMD() {
        setJMeterHome();
        if (isJMeter32orLater()) {
            configureCMDLogging();
        }
    }

    private void configureCMDLogging() {
        try {
            Class cls = Class.forName("org.jmeterplugins.repository.logging.LoggingConfigurator");
            Constructor constructor = cls.getConstructor();
            constructor.newInstance();
        } catch (Throwable ex) {
            System.out.println("Fail to configure logging " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    private void setJMeterHome() {
        if (JMeterUtils.getJMeterHome() == null || JMeterUtils.getJMeterHome().isEmpty()) {
            File self = new File(PluginManagerCMD.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            String home = self.getParentFile().getParentFile().getParent();
            JMeterUtils.setJMeterHome(home);
        }
    }

    @Override
    protected int processParams(ListIterator listIterator) throws UnsupportedOperationException, IllegalArgumentException {
        LoggingManager.setPriority(Priority.INFO);
        if (!listIterator.hasNext()) {
            showHelp(System.out);
            throw new IllegalArgumentException("Command parameter is missing");
        }

        String command = listIterator.next().toString();
        log.info("Command is: " + command);
        try {
            switch (command) {
                case "status":
                    System.out.println(PluginManager.getAllPluginsStatus());
                    break;
                case "install":
                    process(listIterator, true);
                    break;
                case "install-all-except":
                    installAll(listIterator, true);
                    break;
                case "install-for-jmx":
                    installPluginsForJmx(listIterator);
                    break;
                case "uninstall":
                    process(listIterator, false);
                    break;
                case "help":
                    showHelp(System.out);
                    break;
                case "available":
                    System.out.println(PluginManager.getAvailablePluginsAsString());
                    break;
                case "upgrades":
                    System.out.println(PluginManager.getUpgradablePluginsAsString());
                    break;
                default:
                    showHelp(System.out);
                    throw new UnsupportedOperationException("Wrong command: " + command);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to perform cmdline operation: " + e.getMessage(), e);
        }

        return 0;
    }

    private PluginManager getPluginsManager() throws Throwable {
        PluginManager mgr = new PluginManager();
        mgr.setTimeout(30000); // TODO: add property?
        mgr.load();
        return mgr;
    }

    protected void installPluginsForJmx(ListIterator jmxFilesIterator) throws Throwable {
        if (!jmxFilesIterator.hasNext()) {
            throw new IllegalArgumentException("No jmx files specified");
        }
        String files = jmxFilesIterator.next().toString();
        
        PluginManager mgr = getPluginsManager();
        PluginSuggester suggester = new PluginSuggester(mgr);
        final Set<Plugin> pluginsToInstall = new HashSet<>();
        Set<String> jmxFiles = parseParams(files).keySet();
        for (String jmxPath : jmxFiles) {
            pluginsToInstall.addAll(suggester.analyzeTestPlan(jmxPath));
        }

        mgr.togglePlugins(pluginsToInstall, true);
        mgr.applyChanges(this, false, null);
    }

    protected void installAll(ListIterator exclusions, boolean install) throws Throwable {
        Set<String> exceptedPlugins = Collections.emptySet();
        if (exclusions.hasNext()) {
            exceptedPlugins = parseParams(exclusions.next().toString()).keySet();
        }

        PluginManager mgr = getPluginsManager();
        for (Plugin plugin : mgr.getAvailablePlugins()) {
            if (!exceptedPlugins.contains(plugin.getID())) {
                mgr.toggleInstalled(plugin, install);
            }
        }
        mgr.applyChanges(this, false, null);
    }

    protected void process(ListIterator listIterator, boolean install) throws Throwable {
        if (!listIterator.hasNext()) {
            throw new IllegalArgumentException("Plugins list parameter is missing");
        }

        Map<String, String> params = parseParams(listIterator.next().toString());
        PluginManager mgr = getPluginsManager();

        for (Map.Entry<String, String> pluginSpec : params.entrySet()) {
            Plugin plugin = mgr.getPluginByID(pluginSpec.getKey());
            if (pluginSpec.getValue() != null) {
                plugin.setCandidateVersion(pluginSpec.getValue());
            }
            mgr.toggleInstalled(plugin, install);
        }
        mgr.applyChanges(this, false, null);
    }

    private Map<String, String> parseParams(String paramStr) {
        log.info("Params line is: " + paramStr);
        HashMap<String, String> res = new HashMap<>();
        for (String part : paramStr.split(",")) {
            if (part.contains("=")) {
                String[] pieces = part.split("=");
                res.put(pieces[0].trim(), pieces[1].trim());
            } else {
                res.put(part.trim(), null);
            }
        }
        return res;
    }

    @Override
    protected void showHelp(PrintStream printStream) {
        printStream.println("Options for tool 'PluginManagerCMD': <command> <paramstr> "
                + " where <command> is one of: help, status, available, upgrades, install, install-all-except, install-for-jmx, uninstall.");
    }

    @Override
    public void notify(String s) {
        if (s.endsWith("%")) {
            log.debug(s);
        } else {
            log.info(s);
        }
    }
}
