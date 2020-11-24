package org.jmeterplugins.repository;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.gui.util.EscapeDialog;
import org.apache.jorphan.gui.ComponentUtil;
import org.jmeterplugins.repository.exception.DownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedList;

public class PluginManagerDialog extends EscapeDialog implements ActionListener, ComponentListener, HyperlinkListener {
    /**
     *
     */
    private static final long serialVersionUID = 888467568782611707L;
    private static final Logger log = LoggerFactory.getLogger(PluginManagerDialog.class);
    public static final Border SPACING = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    private final PluginManager manager;
    private final JTextPane modifs = new JTextPane();
    private final JButton apply = new JButton("Apply Changes and Restart JMeter");
    private final PluginsList installed;
    private final PluginsList available;
    private final PluginUpgradesList upgrades;
    private final JSplitPane topAndDown = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private final JLabel statusLabel = new JLabel("");
    private final JEditorPane failureLabel = new JEditorPane();
    private final JScrollPane failureScrollPane = new JScrollPane(failureLabel);
    private final ChangeListener cbNotifier;
    private final ChangeListener cbUpgradeNotifier;

    public PluginManagerDialog(PluginManager aManager) {
        super((JFrame) null, "JMeter Plugins Manager", true);
        setLayout(new BorderLayout());
        addComponentListener(this);
        manager = aManager;
        Dimension size = new Dimension(1024, 768);
        setSize(size);
        setPreferredSize(size);
        setIconImage(PluginIcon.getPluginFrameIcon(manager.hasAnyUpdates(), this));
        ComponentUtil.centerComponentInWindow(this);

        failureLabel.setContentType("text/html");
        failureLabel.addHyperlinkListener(this);

        final GenericCallback<Object> statusRefresh = new GenericCallback<Object>() {
            @Override
            public void notify(Object ignored) {
                String changeText = manager.getChangesAsText();
                modifs.setText(changeText);
                apply.setEnabled(!changeText.isEmpty() && installed.isEnabled());
            }
        };

        cbNotifier = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (e.getSource() instanceof PluginCheckbox) {
                    PluginCheckbox checkbox = (PluginCheckbox) e.getSource();
                    Plugin plugin = checkbox.getPlugin();
                    manager.toggleInstalled(plugin, checkbox.isSelected());
                    statusRefresh.notify(this);
                }
            }
        };

        cbUpgradeNotifier = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (e.getSource() instanceof PluginCheckbox) {
                    PluginCheckbox checkbox = (PluginCheckbox) e.getSource();
                    Plugin plugin = checkbox.getPlugin();
                    if (checkbox.isSelected()) {
                        plugin.setCandidateVersion(checkbox.getPlugin().getMaxVersion());
                    } else {
                        plugin.setCandidateVersion(checkbox.getPlugin().getInstalledVersion());
                    }
                    statusRefresh.notify(this);
                }
            }
        };

        installed = new PluginsList(statusRefresh);
        available = new PluginsList(statusRefresh);
        upgrades = new PluginUpgradesList(statusRefresh);

        if (manager.hasPlugins()) {
            setPlugins();
        } else {
            loadPlugins();
        }

        topAndDown.setResizeWeight(.75);
        topAndDown.setDividerSize(5);
        topAndDown.setTopComponent(getTabsPanel());

        topAndDown.setBottomComponent(getBottomPanel());
        add(topAndDown, BorderLayout.CENTER);
        statusRefresh.notify(this); // to reflect upgrades
    }

    private void setPlugins() {
        installed.setPlugins(manager.getInstalledPlugins(), cbNotifier);
        available.setPlugins(manager.getAvailablePlugins(), cbNotifier);
        upgrades.setPlugins(manager.getUpgradablePlugins(), cbUpgradeNotifier);
    }

    private void loadPlugins() {
        if (!manager.hasPlugins()) {
            try {
                manager.load();
                setPlugins();
            } catch (Throwable e) {
                log.error("Failed to load plugins manager", e);
                ByteArrayOutputStream text = new ByteArrayOutputStream(4096);
                e.printStackTrace(new PrintStream(text));
                String msg = "<p>Failed to download plugins repository.<br/>";
                msg += "One of the possible reasons is that you have proxy requirement for Internet connection.</p>" +
                        " Please read the instructions on this page: " +
                        "<a href=\"https://jmeter-plugins.org/wiki/PluginsManagerNetworkConfiguration/\">" +
                        "https://jmeter-plugins.org/wiki/PluginsManagerNetworkConfiguration/</a>" +
                        " <br><br>Error's technical details: <pre>" + text.toString() + "</pre><br>";
                failureLabel.setText("<html>" + msg + "</html>");
                failureLabel.setEditable(false);
                add(failureScrollPane, BorderLayout.CENTER);
                failureLabel.setCaretPosition(0);
            }
        }
    }

    private Component getTabsPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Installed Plugins", installed);
        tabbedPane.addTab("Available Plugins", available);
        tabbedPane.addTab("Upgrades", upgrades);
        return tabbedPane;
    }

    private JPanel getBottomPanel() {
        apply.setEnabled(false);
        modifs.setEditable(false);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));

        JPanel panel = new JPanel(new BorderLayout());

        JPanel modifsPanel = new JPanel(new BorderLayout());
        modifsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, getHeight() / 3));
        modifsPanel.setPreferredSize(new Dimension(getWidth(), getHeight() / 3));
        modifsPanel.setBorder(SPACING);
        modifsPanel.setBorder(BorderFactory.createTitledBorder("Review Changes"));

        modifs.setEditable(false);
        modifsPanel.add(new JScrollPane(modifs), BorderLayout.CENTER);

        panel.add(modifsPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBorder(SPACING);
        btnPanel.add(apply, BorderLayout.EAST);
        btnPanel.add(statusLabel, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        apply.addActionListener(this);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        statusLabel.setForeground(Color.BLACK);
        enableComponents(false);
        new Thread() {
            @Override
            public void run() {
                // FIXME: what to do when user presses "cancel" on save test plan dialog?
                GenericCallback<String> statusChanged = new GenericCallback<String>() {
                    @Override
                    public void notify(final String s) {
                        SwingUtilities.invokeLater(
                                new Runnable() {

                                    @Override
                                    public void run() {
                                        statusLabel.setText(s);
                                        repaint();
                                    }
                                });
                    }
                };
                try {
                    LinkedList<String> options = null;
                    String testPlan = GuiPackage.getInstance().getTestPlanFile();
                    if (testPlan != null) {
                        options = new LinkedList<>();
                        options.add("-t");
                        options.add(testPlan);
                    }
                    manager.applyChanges(statusChanged, true, options);
                    ActionRouter.getInstance().actionPerformed(new ActionEvent(this, 0, ActionNames.EXIT));
                } catch (DownloadException ex) {
                    enableComponents(true);
                    statusLabel.setForeground(Color.RED);
                    statusChanged.notify("Failed to apply changes: " + ex.getMessage());
                } catch (Exception ex) {
                    statusLabel.setForeground(Color.RED);
                    statusChanged.notify("Failed to apply changes: " + ex.getMessage());
                    throw ex;
                }
            }
        }.start();
    }

    private void enableComponents(boolean enable) {
        installed.setEnabled(enable);
        available.setEnabled(enable);
        upgrades.setEnabled(enable);
        apply.setEnabled(enable);
    }

    @Override
    public void componentResized(ComponentEvent e) {

    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent evt) {
        loadPlugins();
        topAndDown.setVisible(!manager.allPlugins.isEmpty());
        failureLabel.setVisible(manager.allPlugins.isEmpty());
        pack();
    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            PluginsList.openInBrowser(e.getURL().toString());
        }
    }
}
