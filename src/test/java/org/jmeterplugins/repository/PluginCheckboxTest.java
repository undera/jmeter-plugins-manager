package org.jmeterplugins.repository;

import org.junit.Test;

import static org.junit.Assert.*;

public class PluginCheckboxTest {

    @Test
    public void testFlow() throws Exception {
        Plugin plugin = new Plugin("id");
        plugin.name  = "plugin-name";
        plugin.canUninstall = true;
        PluginCheckbox checkbox = new PluginCheckbox(plugin.getName());
        checkbox.setPlugin(plugin);

        assertTrue(checkbox.isEnabled());
        checkbox.setEnabled(false);
        assertFalse(checkbox.isEnabled());
        checkbox.setEnabled(true);
        assertTrue(checkbox.isEnabled());

        plugin.canUninstall = false;
        checkbox.setPlugin(plugin);
        assertFalse(checkbox.isEnabled());
        checkbox.setEnabled(true);
        assertFalse(checkbox.isEnabled());

        assertEquals(plugin, checkbox.getPlugin());
    }
}