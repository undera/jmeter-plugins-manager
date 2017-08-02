package org.jmeterplugins.repository.plugins;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.Plugin;
import org.jmeterplugins.repository.PluginManager;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PluginSuggester  {
    private static final Logger log = LoggingManager.getLoggerForClass();

    protected TestPlanAnalyzer analyzer;
    private final PluginManager pmgr;

    public PluginSuggester(PluginManager pmgr) {
        this.pmgr = pmgr;
        analyzer = new TestPlanAnalyzer();
    }

    public void checkAndSuggest(String msg) {
        Set<Plugin> pluginsToInstall = findPluginsToInstall(msg);
        if (pluginsToInstall.size() > 0) {

            togglePlugins(pluginsToInstall);

            Frame parent = (GuiPackage.getInstance() != null) ? GuiPackage.getInstance().getMainFrame() : null;
            SuggestDialog dialog = new SuggestDialog(parent, pmgr, pluginsToInstall);
            dialog.setVisible(true);
            dialog.setAlwaysOnTop(true);
        }
    }

    protected Set<Plugin> findPluginsToInstall(String msg) {
        if (msg != null && msg.contains("Loading file")) {
            String path = msg.substring(msg.indexOf(": ") + 2);
            pmgr.clearAdditionalJMeterOption();
            pmgr.addAdditionalJMeterOptions("-t", path);
            Set<String> nonExistentClasses = analyzer.analyze(path);
            if (nonExistentClasses.size() > 0) {
                return findPluginsFromClasses(nonExistentClasses);
            }
        }
        return Collections.emptySet();
    }

    protected void togglePlugins(Set<Plugin> pluginsToInstall) {
        for (Plugin plugin : pluginsToInstall) {
            pmgr.toggleInstalled(plugin, true);
        }
    }



    protected Set<Plugin> findPluginsFromClasses(Set<String> nonExistentClasses) {
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

}
