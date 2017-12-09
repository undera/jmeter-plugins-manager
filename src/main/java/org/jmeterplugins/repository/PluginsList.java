package org.jmeterplugins.repository;

import org.apache.commons.lang3.StringUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.util.PlaceholderTextField;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class PluginsList extends JPanel implements ListSelectionListener, HyperlinkListener {
    private static final long serialVersionUID = 295116233618658217L;

    private static final Logger log = LoggingManager.getLoggerForClass();

    private final JTextPane description = new JTextPane();
    protected final PlaceholderTextField searchField = new PlaceholderTextField();
    private final DefaultListModel<PluginCheckbox> searchResults = new DefaultListModel<>();
    protected JList<PluginCheckbox> list = new CheckBoxList<>(5);
    private DefaultListModel<PluginCheckbox> listModel = new DefaultListModel<>();
    protected final JComboBox<String> version = new JComboBox<>();
    private ItemListener itemListener = new VerChoiceChanged();
    private GenericCallback<Object> dialogRefresh;

    public PluginsList(GenericCallback<Object> dialogRefresh) {
        super(new BorderLayout(5, 0));
        this.dialogRefresh = dialogRefresh;

        description.setContentType("text/html");
        description.setEditable(false);
        description.addHyperlinkListener(this);

        list.setModel(listModel);
        list.setBorder(PluginManagerDialog.SPACING);
        list.addListSelectionListener(this);

        add(getPluginsListComponent(), BorderLayout.WEST);
        add(getDetailsPanel(), BorderLayout.CENTER);

        list.setComponentPopupMenu(new ToggleAllPopupMenu());
    }

    private Component getPluginsListComponent() {
        initSearchField();
        JPanel topAndDown = new JPanel(new BorderLayout(5, 0));
        topAndDown.add(searchField, BorderLayout.NORTH);
        topAndDown.add(new JScrollPane(list));
        return topAndDown;
    }

    private void initSearchField() {
        searchField.setPlaceholder("Search...");
        searchField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // NOOP.
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // NOOP.
            }

            @Override
            public void keyReleased(KeyEvent e) {
                filterPluginsList();
            }
        });
    }

    private void filterPluginsList() {
        final String filter = searchField.getText().toLowerCase();
        if (!filter.isEmpty()) {
            searchResults.clear();
            for (int i = 0; i < listModel.size(); i++) {
                PluginCheckbox pluginCheckbox = listModel.getElementAt(i);
                Plugin plugin = pluginCheckbox.getPlugin();
                final String data = plugin.getSearchIndexString();
                if (data.contains(filter)) {
                    searchResults.addElement(pluginCheckbox);
                }
            }
            list.setModel(searchResults);
        } else {
            list.setModel(listModel);
        }
    }

    public void setPlugins(Set<Plugin> plugins, ChangeListener checkboxNotifier) {
        listModel.clear();
        for (Plugin plugin : plugins) {
            listModel.addElement(getCheckboxItem(plugin, checkboxNotifier));
        }
    }

    private JPanel getDetailsPanel() {
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.add(new JScrollPane(description), BorderLayout.CENTER);

        version.setEnabled(false);

        JPanel verPanel = new JPanel(new BorderLayout());
        verPanel.add(new JLabel("Version: "), BorderLayout.WEST);
        verPanel.add(version, BorderLayout.CENTER);
        detailsPanel.add(verPanel, BorderLayout.SOUTH);
        return detailsPanel;
    }

    protected PluginCheckbox getCheckboxItem(Plugin plugin, ChangeListener changeNotifier) {
        PluginCheckbox element = new PluginCheckbox(plugin.getName());
        element.setSelected(plugin.isInstalled());
        element.setPlugin(plugin);
        element.addChangeListener(changeNotifier);
        return element;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && list.getSelectedIndex() >= 0) {
            Plugin plugin = list.getSelectedValue().getPlugin();
            description.setText(getDescriptionHTML(plugin));
            setUpVersionsList(list.getSelectedValue());
            setToolTipRenderer(plugin);
            cacheImage(plugin);
            description.setCaretPosition(0);
        }
    }

    private void setToolTipRenderer(Plugin plugin) {
        final List<String> tooltips = new ArrayList<>();

        for (String version : plugin.getVersions()) {
            tooltips.add(plugin.getVersionChanges(version));
        }

        version.setRenderer(new ComboboxToolTipRenderer(tooltips));
    }

    protected void setUpVersionsList(PluginCheckbox cb) {
        version.removeItemListener(itemListener);
        version.removeAllItems();
        for (String ver : cb.getPlugin().getVersions()) {
            version.addItem(ver);
        }
        version.setSelectedItem(getCbVersion(cb));

        version.setEnabled(version.getItemCount() > 1);
        version.addItemListener(itemListener);
    }

    protected String getCbVersion(PluginCheckbox cb) {
        Plugin plugin = cb.getPlugin();
        if (plugin.isInstalled()) {
            return plugin.getInstalledVersion();
        } else {
            return plugin.getCandidateVersion();
        }
    }

    String getDescriptionHTML(Plugin plugin) {
        String txt = "<h1>" + plugin.getName() + "</h1>";

        if (plugin.isUpgradable()) {
            txt += "<p><font color='orange'>This plugin can be upgraded to version " + plugin.getMaxVersion() + "</font></p>";
        }
        if (!plugin.getVendor().isEmpty()) {
            txt += "<p>Vendor: <i>" + plugin.getVendor() + "</i></p>";
        }
        if (!plugin.getDescription().isEmpty()) {
            txt += "<p>" + plugin.getDescription() + "</p>";
        }
        if (!plugin.getHelpLink().isEmpty()) {
            txt += "<p>Documentation: <a href='" + plugin.getHelpLink() + "'>" + plugin.getHelpLink() + "</a></p>";
        }
        String changes = plugin.getVersionChanges(plugin.getCandidateVersion());
        if (null != changes) {
            txt += "<p>What's new in version " + plugin.getCandidateVersion() + ": " + changes + "</p>";
        }
        txt += getMavenInfo(plugin);
        if (!plugin.getScreenshot().isEmpty()) {
            txt += "<p><img src='" + plugin.getScreenshot() + "'/></p>";
        }
        if (plugin.getInstalledPath() != null) {
            txt += "<pre>Location: " + plugin.getInstalledPath() + "</pre>";
        }

        Set<String> deps = plugin.getDepends();
        if (!deps.isEmpty()) {
            txt += "<pre>Dependencies: " + Arrays.toString(deps.toArray(new String[0])) + "</pre>";
        }
        Map<String, String> libs = plugin.getLibs(plugin.getCandidateVersion());
        if (!libs.isEmpty()) {
            txt += "<pre>Libraries: " + Arrays.toString(libs.keySet().toArray(new String[0])) + "</pre>";
        }

        return txt + "<br/>";
    }

    private String getMavenInfo(Plugin plugin) {
        String txt = "";
        if (plugin.getCandidateVersion() != null) {
            String downloadUrl = plugin.getDownloadUrl(plugin.getCandidateVersion());
            int indexOfFP = downloadUrl.indexOf("filepath=");
            if (indexOfFP > 0) {
                String artifactUrl = downloadUrl.substring(indexOfFP + "filepath=".length());
                String[] parts = artifactUrl.split("/");
                String lastVersionId = parts[parts.length - 2];
                String artifactId = parts[parts.length - 3];
                StringBuilder groupId = new StringBuilder();
                for (int i = 0; i < parts.length - 3; i++) {
                    groupId.append(parts[i]).append(".");
                }

                if (!StringUtils.isEmpty(groupId)) {
                    txt += "<p>Maven groupId: <i>" + groupId.substring(0, groupId.length() - 1) + "</i>" +
                            ", artifactId: <i>" + artifactId + "</i>" +
                            ", version: <i>" + lastVersionId + "</i>" +
                            "</p>";
                }
            }
        }
        return txt;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            openInBrowser(e.getURL().toString());
        }
    }

    public static void openInBrowser(String string) {
        if (java.awt.Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().browse(new URI(string));
            } catch (IOException | URISyntaxException ignored) {
                log.debug("Failed to open in browser", ignored);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        list.setEnabled(enabled);
        version.setEnabled(enabled);
        for (PluginCheckbox ch : Collections.list(listModel.elements())) {
            ch.setEnabled(enabled);
        }
    }

    private class VerChoiceChanged implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                if (event.getItem() instanceof String) {
                    String item = (String) event.getItem();
                    Plugin plugin = list.getSelectedValue().getPlugin();
                    plugin.setCandidateVersion(item);
                    dialogRefresh.notify(this);
                    description.setText(getDescriptionHTML(plugin));
                    description.setCaretPosition(0);
                }
            }
        }
    }


    private void cacheImage(Plugin plugin) {
        if (!plugin.getScreenshot().isEmpty()) {
            try {
                Dictionary cache = (Dictionary) description.getDocument().getProperty("imageCache");
                if (cache == null) {
                    cache = new Hashtable();
                    description.getDocument().putProperty("imageCache", cache);
                }

                URL url = new URL(plugin.getScreenshot());
                BufferedImage image = ImageIO.read(url);
                if (image != null) {
                    cache.put(url, image);
                }
            } catch (IOException e) {
                log.warn("Cannot cached image " + plugin.getScreenshot());
            }
        }
    }

    private class ToggleAllPopupMenu extends JPopupMenu implements ActionListener {
        /**
         *
         */
        private static final long serialVersionUID = -4299203920659842279L;

        public ToggleAllPopupMenu() {
            super("Toggle All");
            JMenuItem menuItem = new JMenuItem("Toggle All");
            menuItem.addActionListener(this);
            add(menuItem);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            for (Object a : listModel.toArray()) {
                if (a instanceof PluginCheckbox) {
                    PluginCheckbox cb = (PluginCheckbox) a;
                    cb.doClick();
                }
            }
            list.repaint();
        }
    }

    private class ComboboxToolTipRenderer extends DefaultListCellRenderer {
        private final List<String> tooltips;

        public ComboboxToolTipRenderer(List<String> tooltips) {
            this.tooltips = tooltips;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            JComponent comp = (JComponent) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);

            if (-1 < index && null != value && null != tooltips && tooltips.size() > index) {
                list.setToolTipText(tooltips.get(index));
            }
            return comp;
        }
    }
}

