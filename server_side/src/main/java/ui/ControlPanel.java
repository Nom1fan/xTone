package ui;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/**
 * Created by Mor on 27/03/2016.
 */
public class ControlPanel extends JPanel {

    public ControlPanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(get_lblControlPanel());
        add(Box.createVerticalStrut(20));
        add(get_tabPane());
    }

    private JTabbedPane get_tabPane() {

        JTabbedPane _tabPane = new JTabbedPane();
        _tabPane.add("Server Panel", new ServerPanel());
        _tabPane.add("Sms Panel", new SendSmsPanel());
        _tabPane.add("Push Panel", new SendPushPanel());
        return _tabPane;
    }

    private JLabel get_lblControlPanel() {

        JLabel _lblControlPanel = new JLabel();
        _lblControlPanel.setFont(new Font(null, Font.PLAIN, 20));
        _lblControlPanel.setText("MediaCallz Server Control Panel");
        return _lblControlPanel;
    }


}
