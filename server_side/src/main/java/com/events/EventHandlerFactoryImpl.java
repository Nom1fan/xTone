package com.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mor on 25/07/2016.
 */
@Component
public class EventHandlerFactoryImpl implements EventHandlerFactory {

    private final Map<EventType,EventHandler> map = new HashMap<>();

    @Autowired
    public void initMap(List<EventHandler> eventHandlerList) {
        for (EventHandler handler : eventHandlerList) {
            EventType handledEventType = handler.getHandledEventType();
            map.put(handledEventType, handler);
        }
    }

    @Override
    public EventHandler getEventHandler(EventType eventType) {
        return map.get(eventType);
    }
}
