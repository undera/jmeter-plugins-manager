package org.jmeterplugins.repository.cache;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class PluginsRepoTest {

    @Test
    public void testFlow() throws Exception {
        String serializedRepo = getClass().getResource("/serializedRepo").getFile();
        File file = new File(serializedRepo);
        PluginsRepo repo = PluginsRepo.fromFile(file);
        assertNotNull(repo);
        assertEquals(124810, repo.getRepoJSON().length());
        assertEquals(1526736963000L, repo.getExpirationTime());
        File tempFile = File.createTempFile("tmp_cache", "serialized");
        repo.saveToFile(tempFile);
        assertEquals(file.length(), tempFile.length());
    }

    @Test
    public void testFlow2() throws Exception {
        long l = System.currentTimeMillis();
        PluginsRepo repo = new PluginsRepo("", l + 10000, l);
        assertTrue(repo.isActual());

        repo = new PluginsRepo("", l - 10000, l);
        assertFalse(repo.isActual());
    }
}