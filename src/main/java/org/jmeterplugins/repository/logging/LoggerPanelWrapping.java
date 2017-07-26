package org.jmeterplugins.repository.logging;

import org.apache.jmeter.gui.LoggerPanel;
import org.apache.log.LogEvent;

import javax.swing.*;

public class LoggerPanelWrapping extends LoggerPanel {

    public LoggerPanelWrapping() {
        super();
    }


    @Override
    public void processEvent(LogEvent logEvent) {
        String msg = logEvent.getMessage();
        System.out.println("tostr: " + logEvent.toString() + " msg: " + logEvent.getMessage());
        if (msg != null && msg.contains("Loading file")) {
            JOptionPane.showMessageDialog(null, msg, "title", JOptionPane.INFORMATION_MESSAGE);
        }
    }

}
