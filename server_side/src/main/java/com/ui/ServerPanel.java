package com.ui;

import com.events.Event;
import com.events.EventHandler;
import com.events.EventHandlerFactory;
import com.server.servers.ServerRunner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Dimension;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.PostConstruct;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Created by Mor on 28/03/2016.
 */
@Component
public class ServerPanel extends JPanel implements Observer {

    @Autowired
    private ServerRunner serverRunner;

    @Autowired
    EventHandlerFactory eventHandlerFactory;

    private JLabel lblInfo;

    @PostConstruct
    public void init() {
        serverRunner.addObserver(this);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(200, 200));

        add(getBtnStart());
        add(getBtnStop());
        add(getLblInfo());
    }

    public void setLblInfoText(String text) {
        lblInfo.setText(text);
    }

    private JLabel getLblInfo() {
        lblInfo = new JLabel();
        return lblInfo;
    }

    private JButton getBtnStop() {

        JButton btnStop = new JButton();
        btnStop.setText("Stop server");
        btnStop.addActionListener(e -> serverRunner.stopServer());

        return btnStop;
    }

    private JButton getBtnStart() {

        JButton btnStart = new JButton();
        btnStart.setText("Start server");
        btnStart.addActionListener(e -> {
            serverRunner.runServer();
            lblInfo.setText("Please wait...");
        });

        return btnStart;
    }

    @Override
    public void update(Observable observable, Object data) {
        Event event = (Event)data;
        EventHandler eventHandler = eventHandlerFactory.getEventHandler(event.getEventType());
        eventHandler.handleEvent(this);
    }
}
