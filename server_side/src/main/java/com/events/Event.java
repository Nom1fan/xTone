package com.events;

/**
 * Created by Mor on 25/07/2016.
 */
public class Event {

    private EventType eventType;
    private String source;
    private String msg;
    private Object data;

    public Event(String source, EventType eventType) {
        this.eventType = eventType;
        this.source = source;
    }

    public Event(String source, EventType eventType, String msg) {
        this.source = source;
        this.eventType = eventType;
        this.msg = msg;
    }

    public Event(EventType eventType, String source, String msg, Object data) {
        this.eventType = eventType;
        this.source = source;
        this.msg = msg;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public String getMsg() {
        return msg;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getSource() {
        return source;
    }
}
