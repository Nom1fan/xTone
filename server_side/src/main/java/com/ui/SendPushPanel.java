package com.ui;

import com.server.database.DAO;
import com.server.pushservice.PushSender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import DataObjects.PushEventKeys;
import DataObjects.UserDBO;

/**
 * Created by Mor on 28/03/2016.
 */
@Component
public class SendPushPanel extends JPanel {

    @Autowired
    private DAO dao;

    @Autowired
    private PushSender pushSender;

    private JTextField _txtFieldSendTo;
    private JTextField _txtFieldContent;


    @PostConstruct
    public void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel sendToPanel = new JPanel();
        sendToPanel.setLayout(new BoxLayout(sendToPanel, BoxLayout.Y_AXIS));
        sendToPanel.add(get_lblSendTo());
        sendToPanel.add(get_txtFieldSendTo());

        JPanel msgContentPanel = new JPanel();
        msgContentPanel.setLayout(new BoxLayout(msgContentPanel, BoxLayout.Y_AXIS));
        msgContentPanel.add(get_lblMessageContent());
        msgContentPanel.add(get_txtFieldContent());

        JPanel btnSendPanel = new JPanel();
        btnSendPanel.setLayout(new BoxLayout(btnSendPanel, BoxLayout.X_AXIS));
        btnSendPanel.add(Box.createHorizontalStrut(50));
        btnSendPanel.add(get_btnSend());

        add(Box.createVerticalStrut(20));
        add(sendToPanel);
        add(Box.createVerticalStrut(20));
        add(msgContentPanel);
        add(Box.createVerticalStrut(10));
        add(btnSendPanel);
    }

    private JLabel get_lblMessageContent() {

        return new JLabel("Push content:");
    }

    private JLabel get_lblSendTo() {

        return new JLabel("Send push to:");
    }

    private JTextField get_txtFieldContent() {

        _txtFieldContent = new JTextField();
        _txtFieldContent.setToolTipText("Push message...");
        return _txtFieldContent;
    }

    private JTextField get_txtFieldSendTo() {

        _txtFieldSendTo = new JTextField();
        _txtFieldSendTo.setToolTipText("Send push to...");
        return _txtFieldSendTo;
    }

    private JButton get_btnSend() {

        JButton _btnSend = new JButton();
        _btnSend.setText("Send");
        _btnSend.addActionListener(e -> {
            String dest = _txtFieldSendTo.getText();
            String msg = _txtFieldContent.getText();

            try {
                UserDBO userRecord = dao.getUserRecord(dest);
                pushSender.sendPush(userRecord.getToken(), PushEventKeys.SHOW_MESSAGE, "Notification", msg);
            } catch (SQLException e1) {
                e1.printStackTrace();

            }
        });

        return _btnSend;
    }
}
