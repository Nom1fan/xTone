package ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import DataObjects.SharedConstants;
import servers.GenericServer;

/**
 * Created by Mor on 28/03/2016.
 */
public class ServerPanel extends JPanel {

    private GenericServer _logicServer;
    private GenericServer _storageServer;
    private JLabel _lblInfo;

    public ServerPanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(200, 200));

        add(get_btnStart());
        add(get_btnStop());
        add(get_lblInfo());
    }

    private JLabel get_lblInfo() {

        _lblInfo = new JLabel();
        return _lblInfo;
    }

    private JButton get_btnStop() {

        JButton btnStop = new JButton();
        btnStop.setText("Stop server");
        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if(_logicServer.isListening()) {
                    try {
                        _logicServer.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

                if(_storageServer.isListening()) {
                    try {
                        _storageServer.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

                if(_logicServer.isClosed() && _storageServer.isClosed())
                    _lblInfo.setText("Server stopped.");
            }
        });

        return btnStop;
    }

    private JButton get_btnStart() {

        JButton btnStart = new JButton();
        btnStart.setText("Start server");
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                _logicServer = new GenericServer("LogicServer", SharedConstants.LOGIC_SERVER_PORT);
                _storageServer = new GenericServer("StorageServer", SharedConstants.STORAGE_SERVER_PORT);

                if(_logicServer.isListening() && _storageServer.isListening())
                    _lblInfo.setText("Server running...");
            }
        });

        return btnStart;
    }
}
