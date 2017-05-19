package org.jmeterplugins.repository;

import java.util.Comparator;

public class Library {

    protected String name;
    protected String version;
    protected String condition;
    protected String link;

    public static final Comparator<Library> versionComparator = new Comparator<Library>() {
        @Override
        public int compare(Library lib1, Library lib2) {
            int code = conditionComparator.compare(lib1, lib2);
            if (code != 0) {
                return code;
            }

            final String[] versions1 = lib1.getVersion().split("[.]");
            final String[] versions2 = lib2.getVersion().split("[.]");
            int length = (versions1.length > versions2.length) ? versions2.length : versions1.length;

            for (int i = 0; i < length; i++) {
                code = Integer.compare(Integer.parseInt(versions1[i]), Integer.parseInt(versions2[i]));
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

    public static final Comparator<Library> conditionComparator = new Comparator<Library>() {
        // '==' have higher priority than '>='
        @Override
        public int compare(Library lib1, Library lib2) {
            if ("==".equals(lib1.getCondition()) && ">=".equals(lib2.getCondition())) {
                return 1;
            } else if (">=".equals(lib1.getCondition()) && "==".equals(lib2.getCondition())) {
                return -1;
            } else {
                return 0;
            }
        }
    };



    public Library() {
    }

    public Library(String name, String condition, String version, String link) {
        this.name = name;
        this.condition = condition;
        this.version = version;
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

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getFullName() {
        return name + condition + version;
    }
}