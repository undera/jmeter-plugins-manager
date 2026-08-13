package org.jmeterplugins.repository.cache;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class PluginsRepoTest {

    @Test
    public void testFlow() throws Exception {
        String json = "[{\"id\": \"some-plugin\", \"versions\": {\"1.0\": {}}}]";
        PluginsRepo repo = new PluginsRepo(json, 1526736963000L, 1526736900000L);

        File tempFile = File.createTempFile("tmp_cache", "repo");
        repo.saveToFile(tempFile);

        PluginsRepo loaded = PluginsRepo.fromFile(tempFile);
        assertNotNull(loaded);
        assertEquals(json, loaded.getRepoJSON());
        assertEquals(1526736963000L, loaded.getExpirationTime());
        assertFalse(loaded.isActual());
        assertFalse(loaded.isActual(1526736900000L));
        tempFile.delete();
    }

    @Test
    public void testJSONSurvivesNewlines() throws Exception {
        String json = "[\n  {\"id\": \"pretty-printed\"}\n]";
        PluginsRepo repo = new PluginsRepo(json, System.currentTimeMillis() + 10000, 0);

        File tempFile = File.createTempFile("tmp_cache", "repo");
        repo.saveToFile(tempFile);

        PluginsRepo loaded = PluginsRepo.fromFile(tempFile);
        assertNotNull(loaded);
        assertEquals(json, loaded.getRepoJSON());
        assertTrue(loaded.isActual());
        tempFile.delete();
    }

    /**
     * Caches written by older versions were Java-serialized. They must be ignored rather than
     * deserialized, so the repo is simply re-fetched.
     */
    @Test
    public void testLegacySerializedCacheIsIgnored() throws Exception {
        File legacy = new File(getClass().getResource("/serializedRepo").getFile());
        assertTrue(legacy.exists());
        assertNull(PluginsRepo.fromFile(legacy));
    }

    @Test
    public void testMissingFile() throws Exception {
        assertNull(PluginsRepo.fromFile(new File("/no/such/repo/cache")));
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
