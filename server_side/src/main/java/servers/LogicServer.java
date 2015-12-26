//package servers;
//
//
//import com.almworks.sqlite4java.SQLiteException;
//import com.database.MySqlDAL;
//import com.database.SQLiteDAL1;
//
//import java.nio.file.Paths;
//import java.sql.SQLException;
//
//import DalObjects.IDAL;
//import ServerObjects.ClientsManager;
//
///**
// * Created by Mor on 23/09/2015.
// */
//public class LogicServer extends GenericServer {
//
//    public LogicServer(String serverName, int port) {
//        super(serverName, port);
//    }
//
//    @Override
//    protected IDAL initDAL() throws SQLException {
//        return initMySqlDAL();
//    }
//
//    /* LogicServer private methods */
//
//    private SQLiteDAL1 initSQLiteDAL() throws SQLiteException {
//
//        // Initializing General Database
//        SQLiteDAL1 dal = new SQLiteDAL1(Paths.get("").toAbsolutePath().toString() + com.database.SQLiteDAL1.GENERAL_DB_PATH);
//        // Creating tables
//        dal.createTable(SQLiteDAL1.TABLE_UID2TOKEN, SQLiteDAL1.COL_UID, com.database.SQLiteDAL1.COL_TOKEN);
//
//        return dal;
//    }
//
//    private MySqlDAL initMySqlDAL() throws SQLException {
//
//        return new MySqlDAL();
//    }
//}
