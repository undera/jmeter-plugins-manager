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
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import static org.jmeterplugins.repository.PluginManagerDialog.SPACING;

public class SuggestDialog extends JDialog {

    private final PluginManager manager;
    private JLabel titleLabel = new JLabel("");
    private JLabel statusLabel = new JLabel("");

    public SuggestDialog(Frame parent, PluginManager manager, Set<Plugin> plugins) {
        super(parent, "JMeter Plugins Manager", true);
        setLocationRelativeTo(parent);
        this.manager = manager;
        init(plugins);
    }

    private void init(Set<Plugin> plugins) {
        setLayout(new BorderLayout());
        Dimension size = new Dimension(800, 600);
        setSize(size);
        setPreferredSize(size);
        setResizable(false);
        setIconImage(PluginManagerMenuItem.getPluginsIcon().getImage());
        ComponentUtil.centerComponentInWindow(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0,0));
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

        mainPanel.add(getButtonsPanel(), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        //pack();
    }

    private JPanel getButtonsPanel() {
        JButton btnYes = new JButton("Yes, install it");
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

        JButton btnNo = new JButton("Cancel");
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

}