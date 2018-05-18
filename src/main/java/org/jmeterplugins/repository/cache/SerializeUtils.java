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

public class SerializeUtils {
    private static final Logger log = LoggingManager.getLoggerForClass();

    public static void serialize(PluginsRepo repo, File file) {
        // Serialization
        try {
            FileUtils.touch(file);

            //Saving of object in a file
            FileOutputStream fout = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fout);

            // Method for serialization of object
            out.writeObject(repo);

            out.close();
            fout.close();

        } catch (IOException ex) {
            log.warn("Failed for serialize repo", ex);
        }
    }

    public static PluginsRepo deserialize(File file) {
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
