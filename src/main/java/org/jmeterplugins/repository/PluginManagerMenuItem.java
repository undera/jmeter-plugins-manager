package org.jmeterplugins.repository;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;
import org.apache.jmeter.gui.util.JMeterToolBar;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.logging.LoggingHooker;
import org.jmeterplugins.repository.util.ComponentFinder;

import java.util.Arrays;

public class PluginManagerMenuItem extends JMenuItem implements ActionListener {
    private static final long serialVersionUID = -8708638472918746046L;
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static PluginManagerDialog dialog;
    private final PluginManager mgr;

    public PluginManagerMenuItem() {
        super("Plugins Manager");
        addActionListener(this);

        mgr = new PluginManager(); // don't delay startup for longer that 1 second
        LoggingHooker hooker = new LoggingHooker(mgr);
        hooker.hook();
        final JButton toolbarButton = getToolbarButton();
        addToolbarIcon(toolbarButton);
        setIcon(getPluginsIcon(false));

        new Thread("repo-downloader-thread") {
            @Override
            public void run() {
                try {
                    mgr.load();
                } catch (Throwable e) {
                    log.warn("Failed to load plugin updates info", e);
                }

                if (mgr.hasAnyUpdates()) {
                    setText("Plugins Manager (has upgrades)");
                    log.info("Plugins Manager has upgrades: " + Arrays.toString(mgr.getUpgradablePlugins().toArray()));
                }

                setIcon(getPluginsIcon(mgr.hasAnyUpdates()));
                toolbarButton.setIcon(getIcon22Px(mgr.hasAnyUpdates()));

            }
        }.start();
    }

    private void addToolbarIcon(final Component toolbarButton) {
        GuiPackage instance = GuiPackage.getInstance();
        if (instance != null) {
            final MainFrame mf = instance.getMainFrame();
            final ComponentFinder<JMeterToolBar> finder = new ComponentFinder<>(JMeterToolBar.class);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JMeterToolBar toolbar = null;
                    while (toolbar == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.debug("Did not add btn to toolbar", e);
                        }
                        log.debug("Searching for toolbar");
                        toolbar = finder.findComponentIn(mf);
                    }

                    int pos = toolbar.getComponents().length - 1;
                    toolbarButton.setSize(toolbar.getComponent(pos).getSize());
                    toolbar.add(toolbarButton, pos + 1);
                }
            });
        }
    }

    private JButton getToolbarButton() {
        JButton button = new JButton(getIcon22Px(mgr.hasAnyUpdates()));
        button.setToolTipText("Plugins Manager (has upgrades)");
        button.addActionListener(this);
        return button;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
            dialog = new PluginManagerDialog(mgr);
        }

        dialog.pack();
        dialog.setVisible(true);
    }

    public static ImageIcon getIcon22Px(boolean hasUpdates) {
        if (hasUpdates) {
            return new ImageIcon(PluginManagerMenuItem.class.getResource("/org/jmeterplugins/logo22Update.png"));
        } else {
            return new ImageIcon(PluginManagerMenuItem.class.getResource("/org/jmeterplugins/logo22.png"));
        }
    }

    public static ImageIcon getPluginsIcon(boolean hasUpdates) {
        if (hasUpdates) {
            return new ImageIcon(PluginManagerMenuItem.class.getResource("/org/jmeterplugins/logoUpdate.png"));
        } else {
            return new ImageIcon(PluginManagerMenuItem.class.getResource("/org/jmeterplugins/logo.png"));
        }
    }
}
