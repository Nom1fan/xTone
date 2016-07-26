package com.events;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mor on 25/07/2016.
 */
@Component
public class EventGeneratorImpl implements EventGenerator {

    private List<EventsListener> listeners = new LinkedList<>();

    @Override
    public void register(EventsListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregister(EventsListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void fireEvent(Event event) {
        for (EventsListener listener  : listeners) {
            listener.eventReceived(event);
        }
    }
}
