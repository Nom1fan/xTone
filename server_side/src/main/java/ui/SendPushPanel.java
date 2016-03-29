package ui;

import com.database.MySqlDAL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import DataObjects.PushEventKeys;
import ServerObjects.BatchPushSender;
import ServerObjects.ClientsDataAccess;

/**
 * Created by Mor on 28/03/2016.
 */
public class SendPushPanel extends JPanel {

    private JTextField _txtFieldSendTo;
    private JTextField _txtFieldContent;

    public SendPushPanel() {

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

        JLabel _lblMessageContent = new JLabel("Push content:");
        return _lblMessageContent;
    }

    private JLabel get_lblSendTo() {

        JLabel _lblSendTo = new JLabel("Send push to:");
        return _lblSendTo;
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
        _btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String dest = _txtFieldSendTo.getText();
                String msg = _txtFieldContent.getText();

                String token = ClientsDataAccess.instance(new MySqlDAL()).getUserPushToken(dest);

                BatchPushSender.sendPush(token, PushEventKeys.SHOW_MESSAGE, "Notification", msg);
            }
        });

        return _btnSend;
    }
}
