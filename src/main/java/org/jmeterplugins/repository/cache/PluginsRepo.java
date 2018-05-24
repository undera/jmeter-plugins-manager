package org.jmeterplugins.repository.cache;

import org.apache.commons.io.FileUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PluginsRepo implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggingManager.getLoggerForClass();

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
        return isActual() && lastModified > this.lastModified;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public String getRepoJSON() {
        return repoJSON;
    }

    public void saveToFile(File file) {
        // Serialization
        try {
            FileUtils.touch(file);

            //Saving of object in a file
            FileOutputStream fout = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fout);

            // Method for serialization of object
            out.writeObject(this);

            out.close();
            fout.close();

        } catch (IOException ex) {
            log.warn("Failed for serialize repo", ex);
        }
    }

    public static PluginsRepo fromFile(File file) {
        // Deserialization
        try {
            // Reading the object from a file
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fis);

            // Method for deserialization of object
            PluginsRepo repo = (PluginsRepo) in.readObject();

            in.close();
            fis.close();

            return repo;
        } catch (IOException | ClassNotFoundException ex) {
            log.warn("Failed for deserialize repo", ex);
            return null;
        }
    }
}
