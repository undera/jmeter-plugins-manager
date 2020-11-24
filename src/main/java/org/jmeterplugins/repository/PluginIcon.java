package org.jmeterplugins.repository;

import javax.swing.*;
import java.awt.*;

public class PluginIcon {

    private static boolean SVG_AVAILABLE;
    private static boolean FRAME_ICON_API_AVAILABLE;

    static {
        try {
            Class.forName( "com.github.weisj.darklaf.icons.IconLoader" );
            SVG_AVAILABLE = true;
        } catch (ClassNotFoundException e) {
            SVG_AVAILABLE = false;
        }
        if (SVG_AVAILABLE) {
            try {
                com.github.weisj.darklaf.icons.IconLoader.class.getDeclaredMethod("createFrameIcon", Icon.class, Window.class);
                FRAME_ICON_API_AVAILABLE = true;
            } catch (NoSuchMethodException e) {
                FRAME_ICON_API_AVAILABLE = false;
            }
        }
    }

    public static Image getPluginFrameIcon(boolean hasUpdates, Window window) {
        if (FRAME_ICON_API_AVAILABLE) {
            return com.github.weisj.darklaf.icons.IconLoader.createFrameIcon(getPluginsIcon(hasUpdates), window);
        } else {
            return getPluginImageIcon(hasUpdates, false).getImage();
        }
    }

    public static Icon getIcon22Px(boolean hasUpdates) {
        if (SVG_AVAILABLE) {
            return getPluginsSVGIcon(hasUpdates, 22);
        } else {
            return getPluginImageIcon(hasUpdates, true);
        }
    }

    public static Icon getPluginsIcon(boolean hasUpdates) {
        if (SVG_AVAILABLE) {
            return getPluginsSVGIcon(hasUpdates, 16);
        } else {
            return getPluginImageIcon(hasUpdates, false);
        }
    }

    private static Icon getPluginsSVGIcon(boolean hasUpdates, int size) {
        if (hasUpdates) {
            return com.github.weisj.darklaf.icons.IconLoader.get().getIcon("org/jmeterplugins/logoUpdate.svg", size, size);
        } else {
            return com.github.weisj.darklaf.icons.IconLoader.get().getIcon("org/jmeterplugins/logo.svg", size, size);
        }
    }

    private static ImageIcon getPluginImageIcon(boolean hasUpdates, boolean large) {
        String path = "/org/jmeterplugins/logo";
        if (large) path += "22";
        if (hasUpdates) path += "Update";
        path += ".png";
        return new ImageIcon(PluginIcon.class.getResource(path));
    }
}
