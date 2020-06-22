package org.jmeterplugins.repository.plugins;

import static org.jmeterplugins.repository.PluginManagerDialog.SPACING;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.gui.util.EscapeDialog;
import org.apache.jorphan.gui.ComponentUtil;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.GenericCallback;
import org.jmeterplugins.repository.Plugin;
import org.jmeterplugins.repository.PluginIcon;
import org.jmeterplugins.repository.PluginManager;

public class SuggestDialog extends EscapeDialog implements GenericCallback<String> {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private final PluginManager manager;
    private JLabel titleLabel = new JLabel("");
    private JLabel statusLabel = new JLabel("");

    public SuggestDialog(Frame parent, PluginManager manager, Set<Plugin> plugins, final String testPlan) {
        super(parent, "JMeter Plugins Manager", true);
        setLocationRelativeTo(parent);
        this.manager = manager;
        init(plugins, testPlan);
    }

    private void init(Set<Plugin> plugins, final String testPlan) {
        setLayout(new BorderLayout());
        setIconImage(PluginIcon.getPluginFrameIcon(manager.hasAnyUpdates(),this));
        ComponentUtil.centerComponentInWindow(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBorder(SPACING);

        final StringBuilder message = new StringBuilder("<html><p>Your test plan requires following plugins:</p><ul>");
        for (Plugin plugin : plugins) {
            message.append("<li>").append(plugin.getName()).append("</li>");
        }
        message.append("</ul>");
        message.append("<p></p>");
        message.append("<p>Plugins Manager can install it automatically. Following changes will be applied:</p>");
        message.append("</html>");
        titleLabel.setText(message.toString());
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        mainPanel.add(getDetailsPanel(), BorderLayout.CENTER);

        mainPanel.add(getButtonsPanel(plugins, testPlan), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        pack();
    }

    private JPanel getButtonsPanel(final Set<Plugin> plugins, final String testPlan) {
        final JButton btnYes = new JButton("Yes, install it");
        final JButton btnNo = new JButton("Cancel");
        final SuggestDialog dialog = this;
        btnYes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnYes.setEnabled(false);
                btnNo.setEnabled(false);

                new Thread() {
                    @Override
                    public void run() {
                        LinkedList<String> options = new LinkedList<>();
                        options.add("-t");
                        options.add(testPlan);
                        manager.applyChanges(dialog, true, options);
                        dispose();
                        ActionRouter.getInstance().actionPerformed(new ActionEvent(this, 0, ActionNames.EXIT));
                    }
                }.start();
            }
        });

        btnNo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.togglePlugins(plugins, false);
                dispose();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(btnYes);
        buttons.add(btnNo);
        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBorder(SPACING);
        btnPanel.add(buttons, BorderLayout.EAST);
        btnPanel.add(statusLabel, BorderLayout.WEST);
        return btnPanel;
    }

    private JPanel getDetailsPanel() {
        JTextArea messageLabel = new JTextArea(manager.getChangesAsText());
        messageLabel.setEditable(false);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(SPACING);
        messagePanel.add(new JScrollPane(messageLabel));

        return messagePanel;
    }

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
}