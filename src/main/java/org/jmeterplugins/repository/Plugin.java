package org.jmeterplugins.repository;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Plugin {
    private static final Logger log = LoggerFactory.getLogger(Plugin.class);
    private static final Pattern dependsParser = Pattern.compile("([^=<>]+)([=<>]+[0-9.]+)?");
    public static final String VER_STOCK = "0.0.0-STOCK";
    protected JsonObject versions = new JsonObject();
    protected String id;
    protected String markerClass;
    protected String installedPath;
    protected String installedVersion;
    protected String tempName;
    protected String destName;
    protected String name;
    protected String description;
    protected String screenshot;
    protected String helpLink;
    protected String vendor;
    protected String candidateVersion;
    protected String installerClass = null;
    protected List<String> componentClasses;
    protected boolean canUninstall = true;
    private String searchIndexString;

    public Plugin(String aId) {
        id = aId;
    }

    public static Plugin fromJSON(JsonObject elm) {
        Plugin inst = new Plugin(getString(elm, "id"));
        if (!isNull(elm, "markerClass")) {
            inst.markerClass = getString(elm, "markerClass");
        }
        inst.componentClasses = new ArrayList<>();
        if (inst.markerClass != null) {
            inst.componentClasses.add(inst.markerClass);
        }
        if (elm.has("componentClasses")) {
            JsonArray componentsJSON = elm.getAsJsonArray("componentClasses");
            for (JsonElement component : componentsJSON) {
                inst.componentClasses.add(component.getAsString());
            }
        }
        if (elm.has("versions") && elm.get("versions").isJsonObject()) {
            inst.versions = elm.getAsJsonObject("versions");
        }
        inst.name = getString(elm, "name");
        inst.description = getString(elm, "description");
        if (elm.has("screenshotUrl")) {
            inst.screenshot = getString(elm, "screenshotUrl");
        }
        inst.helpLink = getString(elm, "helpUrl");
        inst.vendor = getString(elm, "vendor");
        if (elm.has("canUninstall")) {
            inst.canUninstall = elm.get("canUninstall").getAsBoolean();
        }
        if (elm.has("installerClass")) {
            inst.installerClass = getString(elm, "installerClass");
        }
        return inst;
    }

    private static boolean isNull(JsonObject elm, String key) {
        return elm.has(key) && elm.get(key).isJsonNull();
    }

    /**
     * Reads a repo field as string, keeping the coercions the repo relies on: numbers and
     * booleans stringify, and an explicit JSON null yields the literal "null" as json-lib did.
     * A field that is absent altogether is a broken repo entry, so it fails loudly.
     */
    private static String getString(JsonObject elm, String key) {
        JsonElement value = elm.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Repository entry has no '" + key + "' field: " + elm);
        }
        return asString(value);
    }

    private static String asString(JsonElement value) {
        return value.isJsonNull() ? "null" : value.getAsString();
    }

    /**
     * A version whose body is absent or JSON null reads as an empty one, so callers see
     * "no depends / no libs / no changes" instead of blowing up. That is what json-lib
     * returned from getJSONObject() for such versions, and the repo does contain them.
     *
     * @return the body of given version, or an empty object when that version is absent or null
     */
    private JsonObject getVersionSpec(String verStr) {
        JsonElement spec = (verStr == null) ? null : versions.get(verStr);
        return (spec != null && spec.isJsonObject()) ? spec.getAsJsonObject() : new JsonObject();
    }

    @Override
    public String toString() {
        return id;
    }

    public void detectInstalled(Set<Plugin> others) {
        if (isVirtual()) {
            detectInstalledVirtual(others);
        } else {
            detectInstalledPlugin();
        }

        if (isInstalled()) {
            candidateVersion = installedVersion;
        } else {
            candidateVersion = getMaxVersion();
        }
    }

    private void detectInstalledPlugin() {
        String path = getJARPath(markerClass);
        if (path != null) {
            try {
                installedPath = URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.warn("Failed decode plugin installed Path ", e);
                installedPath = path;
            }
        }
        if (installedPath != null) {
            installedVersion = getVersionFromPath(installedPath);
            if (installedVersion.equals(VER_STOCK) && isVersionFrozenToJMeter()) {
                installedVersion = getJMeterVersion();
            }

            log.debug("Found plugin " + this + " version " + installedVersion + " at path " + installedPath);
        }
    }

    private void detectInstalledVirtual(Set<Plugin> others) {
        candidateVersion = getMaxVersion();
        log.debug("Detecting virtual " + this + " by depends: " + getDepends());
        for (String depID : getDepends()) {
            installedPath = null;
            for (Plugin plugin : others) {
                if (plugin.getID().equals(depID) && plugin.isInstalled()) {
                    installedPath = "";
                }
            }
            if (installedPath == null) {
                break;
            }
        }

        if (isInstalled()) {
            installedVersion = candidateVersion;
        }
    }

    public boolean isVersionFrozenToJMeter() {
        return versions.has("");
    }

    public String getMaxVersion() {
        Set<String> versions = getVersions();
        if (versions.size() > 0) {
            String[] vers = versions.toArray(new String[0]);
            return vers[vers.length - 1];
        }
        return null;
    }

    public Set<String> getVersions() {
        Set<String> versions = new TreeSet<>(new VersionComparator());
        for (String ver : this.versions.keySet()) {
            if (ver.isEmpty()) {
                versions.add(getJMeterVersion());
            } else {
                versions.add(ver);
            }
        }

        if (isInstalled()) {
            versions.add(installedVersion);
        }
        return versions;
    }

    public static String getJMeterVersion() {
        String ver = JMeterUtils.getJMeterVersion();
        String[] parts = ver.split(" ");
        if (parts.length > 1) {
            return parts[0];
        }

        return ver;
    }

    public static String getVersionFromPath(String installedPath) {
        Pattern p = Pattern.compile("-v?([\\.0-9a-zA-Z]+(-[\\w]+)?).jar");
        Matcher m = p.matcher(installedPath);
        if (m.find()) {
            return m.group(1);
        }
        return VER_STOCK;
    }

    public static String getJARPath(String className) {
        Class<?> cls;
        try {
            log.debug("Trying: " + className);
            cls = Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (Throwable e) {
            if (e instanceof ClassNotFoundException) {
                log.debug("Plugin not found by class: " + className);
            } else {
                log.warn("Unable to load class: " + className, e);
            }
            return null;
        }

        String file = cls.getProtectionDomain().getCodeSource().getLocation().getFile();
        if (!file.toLowerCase().endsWith(".jar")) {
            log.warn("Path is not JAR: " + file);
        }

        return file;
    }

    public static String getLibInstallPath(String lib) {
        String[] cp = System.getProperty(DependencyResolver.JAVA_CLASS_PATH).split(File.pathSeparator);
        String path = getLibPath(lib, cp);
        if (path != null) return path;
        return null;
    }

    public static String getLibPath(String lib, String[] paths) {
        for (String path : paths) {
            Pattern p = Pattern.compile("\\W" + lib + "-([0-9]+\\..+).jar");
            Matcher m = p.matcher(path);
            if (m.find()) {
                log.debug("Found library " + lib + " at " + path);
                return path;
            }
        }
        return null;
    }

    public String getID() {
        return id;
    }

    public String getInstalledPath() {
        return installedPath;
    }

    public String getDestName() {
        return destName;
    }

    public String getTempName() {
        return tempName;
    }

    public boolean isInstalled() {
        return installedPath != null;
    }

    public void download(JARSource jarSource, GenericCallback<String> notify) throws IOException {
        if (isVirtual()) {
            log.debug("Virtual set, won't download: " + this);
            return;
        }

        String version = getCandidateVersion();

        String location = getDownloadUrl(version);

        JARSource.DownloadResult dwn = jarSource.getJAR(id, location, notify);
        tempName = dwn.getTmpFile();
        File f = new File(JMeterEngine.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        destName = URLDecoder.decode(f.getParent(), "UTF-8") + File.separator + dwn.getFilename();
    }

    /**
     * @param version
     * @return
     */
    public String getDownloadUrl(String version) {
        String location;
        if (isVersionFrozenToJMeter()) {
            location = String.format(readDownloadUrl(""), getJMeterVersion());
        } else {
            if (!versions.has(version)) {
                throw new IllegalArgumentException("Version " + version + " not found for plugin " + this);
            }
            location = readDownloadUrl(version);
        }
        return location;
    }

    private String readDownloadUrl(String verStr) {
        JsonElement url = getVersionSpec(verStr).get("downloadUrl");
        if (url == null) {
            throw new IllegalArgumentException("Version " + verStr + " of plugin " + this + " has no downloadUrl");
        }
        return asString(url);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getScreenshot() {
        return screenshot;
    }

    public String getHelpLink() {
        return helpLink;
    }

    public String getVendor() {
        return vendor;
    }

    public String getCandidateVersion() {
        return candidateVersion;
    }

    public boolean canUninstall() {
        return canUninstall;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public void setCandidateVersion(String candidateVersion) {
        this.candidateVersion = candidateVersion;
    }

    public boolean isUpgradable() {
        if (!isInstalled()) {
            return false;
        }

        VersionComparator comparator = new VersionComparator();
        return comparator.compare(getInstalledVersion(), getMaxVersion()) < 0;
    }

    public Set<String> getDepends() {
        Set<String> depends = new HashSet<>();
        JsonObject version = getVersionSpec(getCandidateVersion());
        if (version.has("depends")) {
            JsonArray list = version.getAsJsonArray("depends");
            for (JsonElement o : list) {
                String dep = asString(o);
                Matcher m = dependsParser.matcher(dep);
                if (!m.find()) {
                    throw new IllegalArgumentException("Cannot parse depend str: " + dep);
                }
                depends.add(m.group(1));
            }
        }
        return depends;
    }

    public Map<String, String> getLibs(String verStr) {
        Map<String, String> depends = new HashMap<>();
        JsonObject version = getVersionSpec(isVersionFrozenToJMeter() ? "" : verStr);
        if (version.has("libs")) {
            for (Map.Entry<String, JsonElement> lib : version.getAsJsonObject("libs").entrySet()) {
                depends.put(lib.getKey(), asString(lib.getValue()));
            }
        }
        return depends;
    }

    public Map<String, String> getRequiredLibs(String verStr) {
        Map<String, String> libs = getLibs(verStr);
        Map<String, String> requiredLibs = new HashMap<>();
        for (String libName : libs.keySet()) {
            if (libName.contains(">=")) {
                requiredLibs.put(libName, libs.get(libName));
            }
        }
        return requiredLibs;
    }

    public String getVersionChanges(String versionStr) {
        JsonObject version = getVersionSpec(versionStr);
        return version.has("changes") ?
                asString(version.get("changes")) :
                null;
    }

    public String getInstallerClass() {
        return installerClass;
    }

    public boolean containsComponentClasses(Set<String> classes) {
        for (String cls : componentClasses) {
            if (classes.contains(cls)) {
                return true;
            }
        }
        return false;
    }

    private class VersionComparator implements java.util.Comparator<String> {
        @Override
        public int compare(String a, String b) {
            String[] aParts = a.split("\\W+");
            String[] bParts = b.split("\\W+");

            for (int aN = 0; aN < aParts.length; aN++) {
                if (aN < bParts.length) {
                    int res = compare2(aParts[aN], bParts[aN]);
                    if (res != 0) return res;
                }
            }

            return a.compareTo(b);
        }

        private int compare2(String a, String b) {
            if (a.equals(b)) {
                return 0;
            }

            Object ai, bi;
            try {
                ai = Integer.parseInt(a);
            } catch (NumberFormatException e) {
                ai = a;
            }

            try {
                bi = Integer.parseInt(b);
            } catch (NumberFormatException e) {
                bi = b;
            }

            if (ai instanceof Integer && bi instanceof Integer) {
                return Integer.compare((Integer) ai, (Integer) bi);
            } else if (ai instanceof String && bi instanceof String) {
                return ((String) ai).compareTo((String) bi);
            } else if (ai instanceof String) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public boolean isVirtual() {
        return markerClass == null;
    }

    public String getSearchIndexString() {
        if (searchIndexString == null || searchIndexString.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append(id);
            builder.append(name);
            builder.append(description);
            for (String component : componentClasses) {
                builder.append(component);
            }
            searchIndexString = builder.toString().toLowerCase();
        }

        return searchIndexString;
    }
}
