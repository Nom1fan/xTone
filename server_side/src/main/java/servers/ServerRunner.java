package servers;

import com.database.MySqlDAL;

import java.sql.SQLException;

import DataObjects.SharedConstants;

/**
 * Created by Mor on 16/10/2015.
 */
public class ServerRunner {

    public static void main(String args[]) {

        try {
            MySqlDAL dal = new MySqlDAL();
            new GenericServer("LogicServer", SharedConstants.LOGIC_SERVER_PORT, dal);
            new GenericServer("StorageServer", SharedConstants.STORAGE_SERVER_PORT, dal);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
