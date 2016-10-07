package MessagesToClient;
import java.io.Serializable;

/**
 * Abstract message to the client, containing information and enables generic interface for client actions corresponding to the message
 * @author Mor
 *
 */
public class MessageToClient<T> implements Serializable {

	protected T result;
	protected ClientActionType actionType;

	public MessageToClient(ClientActionType actionType) {

		this.actionType = actionType;
	}

	public MessageToClient(ClientActionType actionType, T result) {

		this.result = result;
		this.actionType = actionType;
	}

	public T getResult() {
		return result;
	}

	public ClientActionType getActionType() {
		return actionType;
	}

	@Override
	public String toString() {
		return "MessageToClient{" +
				"result=" + result +
				", actionType=" + actionType +
				'}';
	}
}
