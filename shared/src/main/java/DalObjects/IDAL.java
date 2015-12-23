package DalObjects;

import java.sql.SQLException;

/**
 * Created by Mor on 16/11/2015.
 */
public interface IDAL {

    boolean registerUser(String uid, String token);
    boolean unregisterUser(String uid);
    boolean updateUserPushToken(String uid, String token);
    int insertCommunicationHistory(String src, String dest, String extension, int size) throws SQLException;
    String getUserPushToken(String uid);

}
