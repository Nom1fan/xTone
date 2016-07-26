package com.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Font;

import javax.annotation.PostConstruct;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Created by Mor on 27/03/2016.
 */
@Component
public class ControlPanel extends JPanel {

    @Autowired
    private ServerPanel serverPanel;

    @Autowired
    private SendSmsPanel sendSmsPanel;

    @Autowired
    private SendPushPanel sendPushPanel;

    @PostConstruct
    public void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getLblControlPanel());
        add(Box.createVerticalStrut(20));
        add(getTabPane());
    }

    private JTabbedPane getTabPane() {

        JTabbedPane tabPane = new JTabbedPane();
        tabPane.add("Server Panel", serverPanel);
        tabPane.add("Sms Panel", sendSmsPanel);
        tabPane.add("Push Panel", sendPushPanel);
        return tabPane;
    }

    private JLabel getLblControlPanel() {

        JLabel lblControlPanel = new JLabel();
        lblControlPanel.setFont(new Font(null, Font.PLAIN, 20));
        lblControlPanel.setText("MediaCallz Server Control Panel");
        return lblControlPanel;
    }


}
