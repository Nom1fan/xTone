package MessagesToServer;

import java.io.IOException;
import MessagesToClient.MessageRegisterRes;
import ServerObjects.UsersDataAccess;
import ServerObjects.SmsVerificationAccess;


public class MessageRegister extends MessageToServer {
	
	private static final long serialVersionUID = 7382209934954570169L;
	private String _pushToken;
    private int _smsCode;
	
	public MessageRegister(String clientId, String pushToken, int smsCode) {
		
		super(clientId);
		_pushToken = pushToken;
        _smsCode = smsCode;
	
	}
	
	@Override
	public boolean doServerAction() throws IOException {
			
		initLogger();				
		
		_logger.info(_messageInitiaterId + " is attempting to register...");

        int expectedSmsCode = SmsVerificationAccess.instance(_dal).getSmsVerificationCode(_messageInitiaterId);

        MessageRegisterRes msgReply;
       if(_smsCode!=-1 && _smsCode == expectedSmsCode) {
           boolean isOK = UsersDataAccess.instance(_dal).registerUser(_messageInitiaterId, _pushToken);
           msgReply = new MessageRegisterRes(isOK);

       } else {
		   _logger.warning("Rejecting registration for [User]:" + _messageInitiaterId +
				   ". [Expected smsCode]:" + expectedSmsCode + " [Received smsCode]:" + _smsCode);
           msgReply = new MessageRegisterRes(false);
       }

        replyToClient(msgReply);

		return _cont;
	}
}

