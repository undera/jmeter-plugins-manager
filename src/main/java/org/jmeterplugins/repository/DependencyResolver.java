package org.jmeterplugins.repository;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyResolver {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final Pattern libNameParser = Pattern.compile("([^=<>]+)([=<>]+[0-9.]+)?");
    public static final String JAVA_CLASS_PATH = "java.class.path";
    protected final Set<Plugin> deletions = new HashSet<>();
    protected final Set<Plugin> additions = new HashSet<>();
    protected final Map<String, String> libAdditions = new HashMap<>();
    protected final Set<String> libDeletions = new HashSet<>();
    protected final Map<Plugin, Boolean> allPlugins;

    public DependencyResolver(Map<Plugin, Boolean> allPlugins) {
        this.allPlugins = allPlugins;
        resolve();
    }

    public void resolve() {
        clear();
        resolveFlags();
        resolveUpgrades();
        resolveDeleteByDependency();
        resolveInstallByDependency();
        resolveDeleteLibs();
        resolveInstallLibs();
        resolveUpgradesLibs();
        detectConflicts();
    }

    public void clear() {
        deletions.clear();
        additions.clear();
        libAdditions.clear();
        libDeletions.clear();
    }


    // TODO: return iterators to make values read-only
    public Set<Plugin> getDeletions() {
        return deletions;
    }

    public Set<Plugin> getAdditions() {
        return additions;
    }

    public Map<String, String> getLibAdditions() {
        return libAdditions;
    }

    public Set<String> getLibDeletions() {
        return libDeletions;
    }

    private Plugin getPluginByID(String id) {
        for (Plugin plugin : allPlugins.keySet()) {
            if (plugin.getID().equals(id)) {
                return plugin;
            }
        }
        throw new RuntimeException("Plugin not found by ID: " + id);
    }

    private Set<Plugin> getDependants(Plugin plugin) {
        Set<Plugin> res = new HashSet<>();
        for (Plugin pAll : allPlugins.keySet()) {
            for (String depID : pAll.getDepends()) {
                if (depID.equals(plugin.getID())) {
                    res.add(pAll);
                }
            }
        }
        return res;
    }

    private void resolveFlags() {
        for (Map.Entry<Plugin, Boolean> entry : allPlugins.entrySet()) {
            if (entry.getKey().isInstalled()) {
                if (!entry.getValue()) {
                    deletions.add(entry.getKey());
                }
            } else if (entry.getValue()) {
                additions.add(entry.getKey());
            }
        }
    }

    private void resolveUpgrades() {
        // detect upgrades
        for (Map.Entry<Plugin, Boolean> entry : allPlugins.entrySet()) {
            Plugin plugin = entry.getKey();
            if (entry.getValue() && plugin.isInstalled() && !plugin.getInstalledVersion().equals(plugin.getCandidateVersion())) {
                log.debug("Upgrade: " + plugin);
                deletions.add(plugin);
                additions.add(plugin);
            }
        }
    }

    private void resolveDeleteByDependency() {
        // delete by depend
        boolean hasModifications = true;
        while (hasModifications) {
            log.debug("Check uninstall dependencies");
            hasModifications = false;
            for (Plugin plugin : deletions) {
                if (!additions.contains(plugin)) {
                    for (Plugin dep : getDependants(plugin)) {
                        if (!deletions.contains(dep) && dep.isInstalled()) {
                            log.debug("Add to deletions: " + dep);
                            deletions.add(dep);
                            hasModifications = true;
                        }
                        if (additions.contains(dep)) {
                            log.debug("Remove from additions: " + dep);
                            additions.remove(dep);
                            hasModifications = true;
                        }
                    }
                }

                if (hasModifications) {
                    break; // prevent ConcurrentModificationException
                }
            }
        }
    }

    private void resolveInstallByDependency() {
        // resolve dependencies
        boolean hasModifications = true;
        while (hasModifications) {
            log.debug("Check install dependencies: " + additions);
            hasModifications = false;
            for (Plugin plugin : additions) {
                for (String pluginID : plugin.getDepends()) {
                    Plugin depend = getPluginByID(pluginID);

                    if (!depend.isInstalled() || deletions.contains(depend)) {
                        if (!additions.contains(depend)) {
                            log.debug("Add to install: " + depend);
                            additions.add(depend);
                            hasModifications = true;
                        }
                    }
                }

                if (hasModifications) {
                    break; // prevent ConcurrentModificationException
                }
            }
        }
    }

    private void resolveInstallLibs() {
        for (Plugin plugin : additions) {
            Map<String, String> libs = plugin.getLibs(plugin.getCandidateVersion());
            for (String lib : libs.keySet()) {
                resolveLibForPlugin(plugin, lib, libs.get(lib));
            }
        }
        resolveLibsVersionsConflicts();
    }

    private void resolveLibForPlugin(Plugin plugin, String lib, String link) {
        String installedPath = Plugin.getLibInstallPath(getLibName(lib));
        if (installedPath == null) {
            libAdditions.put(lib, link);
        } else {
            resolveUpdateLib(plugin, getLibrary(lib, ""), lib);
        }
    }

    private void resolveUpdateLib(Plugin plugin, Library installedLib, String candidateLibName) {
        final Map<String, String> candidateLibs = plugin.getLibs(plugin.getCandidateVersion());

        // get candidate lib
        Library candidateLib = getLibrary(candidateLibName, candidateLibs.get(candidateLibName));

        // get installed lib version
        String installedPath = Plugin.getLibInstallPath(installedLib.getName());
        if (installedPath == null) {
            libAdditions.put(candidateLib.getName(), candidateLib.getLink());
            return;
        }
        String installedVersion = Plugin.getVersionFromPath(installedPath);
        installedLib.setVersion(installedVersion);


        // compare installed and candidate libs
        if (candidateLib.getVersion() != null && Library.versionComparator.compare(installedLib, candidateLib) < 0) {
            libDeletions.add(installedLib.getName());
            libAdditions.put(candidateLib.getName(), candidateLib.getLink());
        }
    }

    private void resolveDeleteLibs() {
        for (Plugin plugin : deletions) {
            if (additions.contains(plugin)) { // skip upgrades
                continue;
            }

            Map<String, String> libs = plugin.getLibs(plugin.getInstalledVersion());
            for (String lib : libs.keySet()) {
                String name = getLibName(lib);
                if (Plugin.getLibInstallPath(name) != null) {
                    libDeletions.add(name);
                } else {
                    log.warn("Did not find library to uninstall it: " + lib);
                }
            }
        }

        for (Plugin plugin : allPlugins.keySet()) {
            if (additions.contains(plugin) || (plugin.isInstalled() && !deletions.contains(plugin))) {
                String ver = additions.contains(plugin) ? plugin.getCandidateVersion() : plugin.getInstalledVersion();
                //log.debug("Affects " + plugin + " v" + ver);
                Map<String, String> libs = plugin.getLibs(ver);
                for (String lib : libs.keySet()) {
                    String name = getLibName(lib);
                    if (libDeletions.contains(name)) {
                        log.debug("Won't delete lib " + lib + " since it is used by " + plugin);
                        libDeletions.remove(name);
                    }
                }
            }
        }
    }

    private void resolveUpgradesLibs() {
        for (Plugin plugin : deletions) {
            if (additions.contains(plugin)) { // if upgrade plugin
                final Map<String, String> installedLibs = plugin.getLibs(plugin.getInstalledVersion());
                final Map<String, String> candidateLibs = plugin.getLibs(plugin.getCandidateVersion());

                for (String candidateLibName : candidateLibs.keySet()) {
                    String installedLibName = getMatchLibName(candidateLibName, installedLibs);
                    if (installedLibName != null) {
                        resolveUpdateLib(plugin, getLibrary(installedLibName, installedLibs.get(installedLibName)), candidateLibName);
                    }
                }

            }
        }
    }

    private String getMatchLibName(String candidateLibName, Map<String, String> installedLibs) {
        final String candidateName = getLibName(candidateLibName);

        for (String installedLibName : installedLibs.keySet()) {
            if (installedLibName.startsWith(candidateName) && getLibName(installedLibName).equals(candidateName)) {
                return installedLibName;
            }
        }

        return null;
    }


    public static String getLibName(String fullLibName) {
        Matcher m = libNameParser.matcher(fullLibName);
        if (!m.find()) {
            throw new IllegalArgumentException("Cannot parse str: " + fullLibName);
        }
        return m.group(1);
    }

    private Library getLibrary(String fullLibName, String link) {
        Matcher m = libNameParser.matcher(fullLibName);
        if (!m.find()) {
            throw new IllegalArgumentException("Cannot parse str: " + fullLibName);
        }

        final String name = m.group(1);
        if (m.groupCount() == 2 && m.group(2) != null && !m.group(2).isEmpty()) {
            String condition = m.group(2).substring(0, 2);
            verifyConditionFormat(condition);
            String version = m.group(2).substring(2);

            return new Library(name, version, link);
        }
        return new Library(name, link);
    }

    private void resolveLibsVersionsConflicts() {
        Map<String, List<Library>> libsToResolve = new HashMap<>();

        for (String key : libAdditions.keySet()) {
            Library library = getLibrary(key, libAdditions.get(key));
            if (library.getVersion() != null) {
                if (libsToResolve.containsKey(library.getName())) {
                    libsToResolve.get(library.getName()).add(library);
                } else {
                    List<Library> libs = new ArrayList<>();
                    libs.add(library);
                    libsToResolve.put(library.getName(), libs);
                }
            }
        }

        for (String key : libsToResolve.keySet()) {
            List<Library> libs = libsToResolve.get(key);
            Collections.sort(libs, Library.versionComparator);

            for (Library lib : libs) {
                libAdditions.remove(lib.getFullName());
            }

            final Library libToInstall = libs.get(libs.size() - 1);
            // override lib
            libAdditions.put(libToInstall.getName(), libToInstall.getLink());
        }
    }


    // TODO: manage '==' and '<=' condition
    protected void verifyConditionFormat(String condition) {
        if (!condition.equals(">=")) {
            throw new IllegalArgumentException("Expected conditions are ['>='], but was: " + condition);
        }
    }

    public void detectConflicts() {
        Set<Plugin> installedPlugins = PluginManager.getInstalledPlugins(allPlugins);

        for (Plugin plugin : installedPlugins) {
            Map<String, String> requiredLibs = plugin.getLibs(plugin.getInstalledVersion());
            for (String lib : requiredLibs.keySet()) {
                resolveLibForPlugin(plugin, lib, requiredLibs.get(lib));
            }
        }
    }
}
