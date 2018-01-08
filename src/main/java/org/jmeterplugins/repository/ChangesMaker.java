package org.jmeterplugins.repository;

import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;

public class ChangesMaker {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private final Map<Plugin, Boolean> allPlugins;

    public ChangesMaker(Map<Plugin, Boolean> allPlugins) {
        this.allPlugins = allPlugins;
    }


    public ProcessBuilder getProcessBuilder(File moveFile, File installFile, File restartFile) throws IOException {
        final ArrayList<String> command = new ArrayList<>();
        command.add(SafeDeleter.getJVM());
        command.add("-classpath");
        command.add(URLDecoder.decode(getTempPmgrJAR().getPath(), "UTF-8"));
        command.add(SafeDeleter.class.getCanonicalName());
        command.add("--move-list");
        command.add(moveFile.getAbsolutePath());
        command.add("--install-list");
        command.add(installFile.getAbsolutePath());

        if (restartFile != null) {
            command.add("--restart-command");
            command.add(restartFile.getAbsolutePath());
        }

        log.debug("Command to execute: " + command);
        final ProcessBuilder builder = new ProcessBuilder(command);
        File cleanerLog = File.createTempFile("jpgc-cleaner-", ".log");
        builder.redirectError(cleanerLog);
        builder.redirectOutput(cleanerLog);
        return builder;
    }

    private File getTempPmgrJAR() throws IOException {
        String jarPath = URLDecoder.decode(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().getFile(), "UTF-8");
        if (!jarPath.endsWith(".jar")) {
            log.warn("Suspicious JAR path detected: " + jarPath);
        }

        File origJAR = new File(jarPath);
        File tempJAR = File.createTempFile(origJAR.getName(), ".jar");
        tempJAR.delete();
        Files.copy(origJAR.toPath(), tempJAR.toPath());
        return tempJAR;
    }


    public File getRestartFile(LinkedList<String> additionalJMeterOptions) throws IOException {
        File file = File.createTempFile("jpgc-restart-", ".list");
        try (PrintWriter out = new PrintWriter(file)) {

            out.print(SafeDeleter.getJVM() + "\n");

            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            List<String> jvmArgs = runtimeMXBean.getInputArguments();
            for (String arg : jvmArgs) {
                out.print(arg + "\n");
            }

            out.print("-jar\n");

            out.print(getJMeterStartCommand(additionalJMeterOptions));

            return file;
        }
    }

    private String getJMeterStartCommand(LinkedList<String> additionalJMeterOptions) {
        StringBuilder cmd = new StringBuilder(JMeterUtils.getJMeterBinDir() + File.separator + "ApacheJMeter.jar\n");
        if (additionalJMeterOptions != null) {
            for (String option : additionalJMeterOptions) {
                cmd.append(option).append("\n");
            }
        }
        return cmd.toString();
    }

    public File getInstallFile(Set<Plugin> plugins) throws IOException {
        File file = File.createTempFile("jpgc-installers-", ".list");
        try (PrintWriter out = new PrintWriter(file)) {
            for (Plugin plugin : plugins) {
                String cls = plugin.getInstallerClass();
                if (cls != null) {
                    log.debug("Plugin " + plugin + " has installer: " + cls);
                    out.print(plugin.getDestName() + "\t" + cls + "\n");
                }
            }
            return file;
        }
    }


    public File getMovementsFile(Set<Plugin> deletes, Set<Plugin> installs, Map<String, String> installLibs, Set<String> libDeletions) throws IOException {
        final File file = File.createTempFile("jpgc-jar-changes", ".list");
        try (PrintWriter out = new PrintWriter(file)) {

            if (!deletes.isEmpty() || !libDeletions.isEmpty()) {
                File delDir = File.createTempFile("jpgc-deleted-jars-", "");
                delDir.delete();
                delDir.mkdir();
                log.info("Will move deleted JARs to directory " + delDir);
                for (Plugin plugin : deletes) {
                    File installed = new File(plugin.getInstalledPath());
                    String delTo = delDir + File.separator + installed.getName();
                    out.print(plugin.getInstalledPath() + "\t" + delTo + "\n");
                }

                for (String lib : libDeletions) {
                    for (Plugin plugin : allPlugins.keySet()) {
                        if (plugin.isInstalled() && plugin.getInstalledPath().equals(lib)) {
                            log.warn("Cannot delete " + lib + " since it is part of plugin " + plugin);
                            libDeletions.remove(lib);
                        }
                    }
                }

                for (String lib : libDeletions) {
                    File installed = new File(lib);
                    String delTo = delDir + File.separator + installed.getName();
                    out.print(lib + "\t" + delTo + "\n");
                }
            }

            String libPath = new File(JOrphanUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParent();
            for (Map.Entry<String, String> lib : installLibs.entrySet()) {
                out.print(lib.getKey() + "\t" + URLDecoder.decode(libPath, "UTF-8") + File.separator + lib.getValue() + "\n");
            }

            for (Plugin plugin : installs) {
                out.print(plugin.getTempName() + "\t" + plugin.getDestName() + "\n");
            }
            return file;
        }
    }
}
