package org.jmeterplugins.repository.plugins;

import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jorphan.gui.ComponentUtil;
import org.jmeterplugins.repository.GenericCallback;
import org.jmeterplugins.repository.Plugin;
import org.jmeterplugins.repository.PluginManager;
import org.jmeterplugins.repository.PluginManagerMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import static org.jmeterplugins.repository.PluginManagerDialog.SPACING;

public class SuggestDialog extends JDialog {

    private final PluginManager manager;
    private JLabel statusLabel = new JLabel("");

    public SuggestDialog(Frame parent, PluginManager manager, Set<Plugin> plugins) {
        super(parent, "JMeter Plugins Manager", true);
        setLocationRelativeTo(parent);
        this.manager = manager;
        init(plugins);
    }

    private void init(Set<Plugin> plugins) {
        setLayout(new BorderLayout());
        Dimension size = new Dimension(450, 250);
        setSize(size);
        setPreferredSize(size);
        setResizable(false);
        setIconImage(PluginManagerMenuItem.getPluginsIcon().getImage());
        ComponentUtil.centerComponentInWindow(this);

        JPanel panel = createPanel(plugins);
        add(panel);
        pack();
    }

    private JPanel createPanel(Set<Plugin> plugins) {
        Dimension size = new Dimension(450, 170);
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSize(size);
        mainPanel.setPreferredSize(size);

        JTextArea messageLabel = new JTextArea(generateMessage(plugins));
        messageLabel.setEditable(false);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setMaximumSize(size);
        messagePanel.setPreferredSize(size);
        messagePanel.setBorder(SPACING);
        messagePanel.add(new JScrollPane(messageLabel));

        mainPanel.add(messagePanel);


        JButton btnYes = new JButton("Yes");
        btnYes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.applyChanges(new GenericCallback<String>() {
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
                });
                ActionRouter.getInstance().actionPerformed(new ActionEvent(this, 0, ActionNames.EXIT));
            }
        });

        JButton btnNo = new JButton("No");
        btnNo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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
        mainPanel.add(btnPanel);
        return mainPanel;
    }

    protected String generateMessage(Set<Plugin> plugins) {
        final StringBuilder message = new StringBuilder("Your JMeter does not have next plugins to open this test plan: \r\n");
        for (Plugin plugin : plugins) {
            message.append("- '").append(plugin.getName()).append("'\r\n");
        }
        message.append("Do you want to install plugins automatically?\r\nWill be applied next changes: \r\n");

        message.append(manager.getChangesAsText());
        return message.toString();
    }
}