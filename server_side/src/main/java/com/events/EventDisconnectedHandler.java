package com.events;

import com.ui.ServerPanel;

import org.springframework.stereotype.Component;

/**
 * Created by Mor on 25/07/2016.
 */
@Component
public class EventDisconnectedHandler implements EventHandler {
    @Override
    public void handleEvent(Object... params) {
        ServerPanel serverPanel = (ServerPanel) params[0];
        serverPanel.setLblInfoText("Server Stopped");
    }

    @Override
    public EventType getHandledEventType() {
        return EventType.DISCONNECTED;
    }
}
