package EventObjects;

import java.io.Serializable;

public class EventReport implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -224696528176497072L;
	private EventType status;
	private String desc;
	private Object data;
	
	public EventReport(EventType status, String desc, Object data) {
		this.status = status;
		this.desc = desc;
		this.data = data;
		
	}

	public EventReport(EventType status, Object data) {
		this.status = status;
		this.data = data;
	}

	public EventReport(EventType status, String desc) {
		this.desc = desc;
		this.status = status;
	}

    public EventReport(EventType status) {
        this.status = status;
    }

    public EventType status() {
		return status;
	}
	
	public String desc() {
		return desc;
	}
	
	public Object data() {
		return data;
	}

	@Override
	public String toString() {

		String str;
		if(desc !=null)
			str = String.format("[EventType]: %s [EventDesc]: %s [EventData]: %s", status, desc, desc);
		else
			str = String.format("[EventType]: %s [EventData]: %s", status, data);
		return str;
	}

	public void setData(String data) {
		this.data = data;
	}
}
