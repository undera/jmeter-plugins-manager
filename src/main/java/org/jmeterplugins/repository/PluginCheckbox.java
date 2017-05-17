package org.jmeterplugins.repository;

import javax.swing.JCheckBox;


class PluginCheckbox extends JCheckBox {

    /**
     *
     */
    private static final long serialVersionUID = 3604852617806921883L;
    private Plugin plugin;

    public PluginCheckbox(String name) {
        super(name);
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
        if (!plugin.canUninstall()) {
            super.setEnabled(false);
        }
    }

    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void setEnabled(boolean b) {
        if (!plugin.canUninstall()) {
            super.setEnabled(false);
        } else {
            super.setEnabled(b);
        }
    }
}
