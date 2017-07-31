package org.jmeterplugins.repository.plugins;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.GenericCallback;
import org.jmeterplugins.repository.Plugin;
import org.jmeterplugins.repository.PluginManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PluginSuggester implements GenericCallback<String> {
    private static final Logger log = LoggingManager.getLoggerForClass();

    protected TestPlanAnalyzer analyzer;

    public PluginSuggester() {
        analyzer = new TestPlanAnalyzer();
    }

    public void checkAndSuggest(String msg) {
        Set<Plugin> pluginsToInstall = findPluginsToInstall(msg);
        if (pluginsToInstall.size() > 0) {

            PluginManager pmgr = PluginManager.getStaticManager();
            togglePlugins(pluginsToInstall);

            Component parent = (GuiPackage.getInstance() != null) ? GuiPackage.getInstance().getMainFrame() : null;
            int n = JOptionPane.showConfirmDialog(parent, generateMessage(pluginsToInstall),
                    "JMeter Plugins Manager: Your JMeter missing some plugins", JOptionPane.YES_NO_OPTION);

            if (n == JOptionPane.YES_OPTION) {
                pmgr.applyChanges(this);
                ActionRouter.getInstance().actionPerformed(new ActionEvent(this, 0, ActionNames.EXIT));
            }
        }
    }

    private Set<Plugin> findPluginsToInstall(String msg) {
        if (msg != null && msg.contains("Loading file")) {
            String path = msg.substring(msg.indexOf(": ") + 2);
            Set<String> nonExistentClasses = analyzer.analyze(path);
            if (nonExistentClasses.size() > 0) {
                return findPluginsFromClasses(nonExistentClasses);
            }
        }
        return Collections.emptySet();
    }

    private void togglePlugins(Set<Plugin> pluginsToInstall) {
        PluginManager pmgr = PluginManager.getStaticManager();
        for (Plugin plugin : pluginsToInstall) {
            pmgr.toggleInstalled(plugin, true);
        }
    }

    private String generateMessage(Set<Plugin> pluginsToInstall) {
        final StringBuilder message = new StringBuilder("Your JMeter does not have next plugins to open this test plan: \r\n");
        for (Plugin plugin : pluginsToInstall) {
            message.append("- '").append(plugin.getName()).append("'\r\n");
        }
        message.append("Do you want to install plugins automatically? Will be applied next changes: \r\n");

        message.append(PluginManager.getStaticManager().getChangesAsText());
        return message.toString();
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
        for (Plugin plugin : availablePlugins) {
            if (plugin.containsComponentClasses(nonExistentClasses)) {
                pluginsToInstall.add(plugin);
            }
        }
        return pluginsToInstall;
    }

    public TestPlanAnalyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(TestPlanAnalyzer analyzer) {
        this.analyzer = analyzer;
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
