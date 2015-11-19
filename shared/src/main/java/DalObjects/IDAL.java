package DalObjects;

/**
 * Created by Mor on 16/11/2015.
 */
public interface IDAL {

    boolean registerUser(String uid, String token);
    boolean unregisterUser(String uid);
    String getUserPushToken(String uid);
    boolean updateUserPushToken(String uid, String token);
}
