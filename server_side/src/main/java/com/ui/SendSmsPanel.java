package com.ui;

import com.server.lang.StringsFactory;
import com.server.sms_service.SmsSender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Created by Mor on 28/03/2016.
 */
@Component
public class SendSmsPanel extends JPanel {

    private JTextField txtFieldSendTo;
    private JTextField txtFieldContent;

    @Autowired
    private SmsSender smsSender;

    @PostConstruct
    public void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel getSmsCodePanel = new JPanel();
        getSmsCodePanel.setLayout(new BoxLayout(getSmsCodePanel, BoxLayout.Y_AXIS));

        JPanel sendToPanel = new JPanel();
        sendToPanel.setLayout(new BoxLayout(sendToPanel, BoxLayout.Y_AXIS));
        sendToPanel.add(getLblSendTo());
        sendToPanel.add(getTxtFieldSendTo());

        JPanel msgContentPanel = new JPanel();
        msgContentPanel.setLayout(new BoxLayout(msgContentPanel, BoxLayout.Y_AXIS));
        msgContentPanel.add(getLblMessageContent());
        msgContentPanel.add(getTxtFieldContent());

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

    private JLabel getLblMessageContent() {

        JLabel lblMessageContent = new JLabel("SMS content:");
        return lblMessageContent;
    }

    private JLabel getLblSendTo() {

        JLabel lblSendTo = new JLabel("Send SMS to:");
        return lblSendTo;
    }

    private JTextField getTxtFieldContent() {

        txtFieldContent = new JTextField();
        txtFieldContent.setToolTipText("Sms message...");
        return txtFieldContent;
    }

    private JTextField getTxtFieldSendTo() {

        txtFieldSendTo = new JTextField();
        txtFieldSendTo.setToolTipText("Send SMS to...");
        return txtFieldSendTo;
    }

    private JButton get_btnSend() {

        JButton _btnSend = new JButton();
        _btnSend.setText("Send");
        _btnSend.addActionListener(e -> {
            String dest = txtFieldSendTo.getText();
            String msg = txtFieldContent.getText();
            smsSender.sendSms(dest, msg);
        });

        return _btnSend;
    }
}
