package org.jmeterplugins.repository.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Cached copy of a repository response, held as a header line followed by the raw JSON.
 * <p>
 * This used to be a Java-serialized object. That cost more to read than parsing the JSON it
 * carried, and it meant calling readObject() on a file in a shared temp directory - a place
 * another local user can get to. Plain text avoids both.
 */
public class PluginsRepo {
    private static final Logger log = LoggerFactory.getLogger(PluginsRepo.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * Leading token of the header line. Bump it if the layout below changes, so that caches
     * written by an older version are rejected and simply re-fetched.
     */
    private static final String FORMAT = "jpgc-repo-cache/1";

    private final String repoJSON;
    private final long expirationTime;
    private final long lastModified;

    public PluginsRepo(String repoJSON, long expirationTime, long lastModified) {
        this.repoJSON = repoJSON;
        this.expirationTime = expirationTime;
        this.lastModified = lastModified;
    }

    public boolean isActual() {
        return expirationTime > System.currentTimeMillis();
    }

    public boolean isActual(long lastModified) {
        return isActual() && lastModified <= this.lastModified;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public String getRepoJSON() {
        return repoJSON;
    }

    public void saveToFile(File file) {
        log.debug("Saving repo to file: " + file.getAbsolutePath());
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), UTF8)) {
            out.write(FORMAT + " " + expirationTime + " " + lastModified + "\n");
            out.write(repoJSON);
        } catch (IOException ex) {
            log.warn("Failed to save repo cache", ex);
        }
    }

    /**
     * @return the cached repo, or null when the file is unreadable or not in the current format,
     * in which case the caller refetches
     */
    public static PluginsRepo fromFile(File file) {
        log.debug("Loading repo from file: " + file.getAbsolutePath());
        try {
            String content = new String(Files.readAllBytes(file.toPath()), UTF8);
            int eol = content.indexOf('\n');
            if (eol < 0) {
                log.warn("Repo cache has no header line, ignoring it: " + file.getAbsolutePath());
                return null;
            }

            String[] header = content.substring(0, eol).split(" ");
            if (header.length != 3 || !FORMAT.equals(header[0])) {
                log.info("Repo cache is not in the current format, ignoring it: " + file.getAbsolutePath());
                return null;
            }

            return new PluginsRepo(content.substring(eol + 1),
                    Long.parseLong(header[1]), Long.parseLong(header[2]));
        } catch (IOException | NumberFormatException ex) {
            log.warn("Failed to read repo cache", ex);
            return null;
        }
    }
}
