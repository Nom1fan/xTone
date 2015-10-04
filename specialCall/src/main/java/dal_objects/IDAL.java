package dal_objects;

import java.sql.ResultSet;

/**
 * Created by mor on 29/09/2015.
 */
public interface IDAL {

    void setLoggedIn(boolean state);
    boolean getLoggedIn();
}
