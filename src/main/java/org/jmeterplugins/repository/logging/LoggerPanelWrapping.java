package org.jmeterplugins.repository.logging;

import org.apache.jmeter.gui.LoggerPanel;
import org.apache.log.LogEvent;
import org.jmeterplugins.repository.plugins.TestPlanAnalyzer;

import javax.swing.*;

public class LoggerPanelWrapping extends LoggerPanel {

    protected TestPlanAnalyzer analyzer;

    public LoggerPanelWrapping() {
        super();
        analyzer = new TestPlanAnalyzer();
    }


    @Override
    public void processEvent(LogEvent logEvent) {
        String msg = logEvent.getMessage();
        System.out.println("tostr: " + logEvent.toString() + " msg: " + logEvent.getMessage());
        if (msg != null && msg.contains("Loading file")) {
            JOptionPane.showMessageDialog(null, msg, "title", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public TestPlanAnalyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(TestPlanAnalyzer analyzer) {
        this.analyzer = analyzer;
    }
}
