package org.jmeterplugins.repository;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import org.junit.Test;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PluginsListTest {

    @Test
    public void testFlow() throws Exception {
        String imgPath = "file:///" + new File(".").getAbsolutePath() + "/target/classes/org/jmeterplugins/logo.png";
        String str = "{\"id\": 0,  \"markerClass\": \"" + PluginsListTest.class.getName() + "\"," +
                " \"screenshotUrl\": \"" + imgPath + "\", \"name\": 3, \"description\": 4, \"helpUrl\": 5, \"vendor\": 5, \"installerClass\": \"test\", " +
                "\"versions\" : { \"0.1\" : { \"changes\": \"fix verified exception1\" }," +
                "\"0.2\" : { \"changes\": \"fix verified exception1\" }," +
                "\"0.3\" : { \"changes\": \"fix verified exception1\" } }}";
        Plugin p = Plugin.fromJSON(JSONObject.fromObject(str, new JsonConfig()));

        Set<Plugin> set = new HashSet<>();
        set.add(p);

        PluginsListExt pluginsList = new PluginsListExt(set, null, null);
        pluginsList.getList().setSelectedIndex(0);

        ListSelectionEvent event = new ListSelectionEvent("0.2", 0, 2, false);
        pluginsList.valueChanged(event);

        p.setCandidateVersion("0.3");
        assertEquals("0.3", pluginsList.getCbVersion(pluginsList.getCheckboxItem(p, null)));

        p.detectInstalled(null);
        assertEquals(Plugin.VER_STOCK, pluginsList.getCbVersion(pluginsList.getCheckboxItem(p, null)));

        pluginsList.setEnabled(false);
        assertFalse(pluginsList.getList().isEnabled());
        assertFalse(pluginsList.getVersion().isEnabled());
        assertFalse(pluginsList.getList().getModel().getElementAt(0).isEnabled());

        JTextField searchField = pluginsList.searchField;
        ListModel model = pluginsList.getList().getModel();
        searchField.setText("not found plugin");
        KeyEvent keyEvent = new KeyEvent(pluginsList.searchField, KeyEvent.KEY_RELEASED, 20, 1, KeyEvent.VK_Z, 'z');
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(searchField, keyEvent);
        searchField.dispatchEvent(keyEvent);
        assertEquals(0, pluginsList.getList().getModel().getSize());
        assertFalse(model == pluginsList.getList().getModel());

        searchField.setText("");
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(searchField, keyEvent);
        searchField.dispatchEvent(keyEvent);
        assertEquals(1, pluginsList.getList().getModel().getSize());
        assertEquals(model, pluginsList.getList().getModel());
    }


    public static class PluginsListExt extends PluginsList {
        public PluginsListExt(Set<Plugin> plugins, ChangeListener checkboxNotifier, GenericCallback<Object> dialogRefresh) {
            super(dialogRefresh);
            setPlugins(plugins, checkboxNotifier);
        }

        public JList<PluginCheckbox> getList() {
            return list;
        }

        public JComboBox<String> getVersion() {
            return version;
        }
    }
}