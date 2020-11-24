package org.jmeterplugins.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class Library {
    private static final Logger log = LoggerFactory.getLogger(Library.class);

    protected String name;
    protected String version;
    protected String link;

    public static final Comparator<Library> versionComparator = new Comparator<Library>() {
        @Override
        public int compare(Library lib1, Library lib2) {
            int code = 0;

            final String[] versions1 = lib1.getVersion().split("[.]");
            final String[] versions2 = lib2.getVersion().split("[.]");
            int length = Math.min(versions1.length, versions2.length);

            for (int i = 0; i < length; i++) {
                try {
                    code = Integer.compare(Integer.parseInt(versions1[i]), Integer.parseInt(versions2[i]));
                } catch (NumberFormatException e) {
                    log.debug("Cannot parse library version", e);
                    code = versions1[i].compareTo(versions2[i]);
                }
                if (code != 0) {
                    break;
                }
            }

            // if version1: 9.9 and version2: 9.9.1
            if (code == 0 && versions1.length != versions2.length) {
                code = Integer.compare(versions1.length, versions2.length);
            }

            return code;
        }
    };


    public Library() {
    }

    public Library(String name, String version, String link) {
        this.name = name;
        this.version = version;
        this.link = link;
    }

    public Library(String name, String link) {
        this.name = name;
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getFullName() {
        return name + ">=" + version;
    }

    public static String getVersionFromFullName(String fullName) {
        return fullName.substring(fullName.indexOf(">=") + 2);
    }

    public static String getNameFromFullName(String fullName) {
        return fullName.substring(0, fullName.indexOf(">="));
    }

    public static class InstallationInfo {
        private final String name;
        private final String tmpPath;
        private final String destinationFileName;

        public InstallationInfo(String name, String tmpPath, String destinationFileName) {
            this.name = name;
            this.tmpPath = tmpPath;
            this.destinationFileName = destinationFileName;
        }

        public String getName() {
            return name;
        }

        public String getTmpPath() {
            return tmpPath;
        }

        public String getDestinationFileName() {
            return destinationFileName;
        }
    }
}
