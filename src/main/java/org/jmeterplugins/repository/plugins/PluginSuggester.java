package org.jmeterplugins.repository.plugins;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.Plugin;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.PluginManagerDialog;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PluginSuggester {
    private static final Logger log = LoggingManager.getLoggerForClass();

    protected TestPlanAnalyzer analyzer;

    public PluginSuggester() {
        analyzer = new TestPlanAnalyzer();
    }

    // TODO: set MINIMUM check interval
    public void checkAndSuggest(String msg) {
        if (msg != null && msg.contains("Loading file")) {
            String path = msg.substring(msg.indexOf(": ") + 2);
            Set<String> nonExistentClasses = analyzer.analyze(path);
            if (nonExistentClasses.size() > 0) {
                Set<Plugin> pluginsToInstall = findPluginsFromClasses(nonExistentClasses);
                if (pluginsToInstall.size() > 0) {
                    StringBuilder message = new StringBuilder("Your JMeter does not have next plugins to open this test plan: \r\n");
                    for (Plugin plugin : pluginsToInstall) {
                        message.append("- '").append(plugin.getName()).append("'\r\n");
                    }
                    message.append("Press 'Install' button to open 'Plugins Manager' and install missing plugins");

                    // TODO: show message windows relative to MAIN window
                    int n = JOptionPane.showOptionDialog(
                            createDialog(),
                            message.toString(),
                            "Attention! Your JMeter missing some plugins",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            new Object[]{"Install", "Cancel"},
                            null);

                    // if click 'Install'
                    if (n == JOptionPane.YES_OPTION) {
                        PluginManagerDialog dialog = new PluginManagerDialog(PluginManager.getStaticManager());
                        dialog.selectPluginsToInstall(pluginsToInstall);
                        dialog.setVisible(true);
                        dialog.setAlwaysOnTop(true);

                        System.out.println("PLS INSTALL");
                    }
                }
            }
        }
    }

    private Component createDialog() {
        final JDialog dialog = new JDialog();
        dialog.setAlwaysOnTop(true);
        return dialog;
    }

    private Set<Plugin> findPluginsFromClasses(Set<String> nonExistentClasses) {
        PluginManager pmgr = PluginManager.getStaticManager();
        try {
            pmgr.load();
        } catch (Throwable throwable) {
            log.warn("Cannot load plugins repo: ", throwable);
            return Collections.emptySet();
        }
        final Set<Plugin> availablePlugins = pmgr.getAvailablePlugins();
        final Set<Plugin> pluginsToInstall = new HashSet<>();
        for (String pluginClass : nonExistentClasses) {
            Plugin plugin = findPlugin(pluginClass, availablePlugins);
            if (plugin != null && !pluginsToInstall.contains(plugin)) {
                pluginsToInstall.add(plugin);
            }
        }
        return pluginsToInstall;
    }

    // TODO: move it into plugin.
    private Plugin findPlugin(String pluginClass, Set<Plugin> availablePlugins) {
        for (Plugin plugin : availablePlugins) {
            if (pluginClass.equals(plugin.getMarkerClass())) {
                return plugin;
            }
        }
        log.warn("Class " + pluginClass + " does not belong to any plugin");
        return null;
    }

    public TestPlanAnalyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(TestPlanAnalyzer analyzer) {
        this.analyzer = analyzer;
    }
}
