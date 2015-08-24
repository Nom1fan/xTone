package EventObjects;

import java.io.Serializable;

public class EventReport implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -224696528176497072L;
	private EventType _status;
	private String _desc;
	private Object _data;
	
	public EventReport(EventType status, String desc, Object data) {
		_status = status;
		_desc = desc;
		_data = data;
		
	}
	
	public EventType status() {
		return _status;
	}
	
	public String desc() {
		return _desc;
	}
	
	public Object data() {
		return _data;
	}

}
