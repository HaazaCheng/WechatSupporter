package net.sklcc.db;

import org.logicalcobwebs.proxool.configuration.JAXPConfigurator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Hazza on 2016/10/9.
 */
public class DBManager {

    private DBManager() {
        try {
//        JAXPConfigurator.configure("/home/sklcc/hazza/wechat/proxool.xml", false);
        JAXPConfigurator.configure("D:\\codes\\WechatSupporterStable\\src/proxool.xml", false);
            Class.forName("org.logicalcobwebs.proxool.ProxoolDriver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection(String poolName) throws SQLException {
        return DriverManager.getConnection(poolName);
    }

    public static DBManager getDBManger() {
        return DBMangerDao.dbManger;
    }

    private static class DBMangerDao {
        private static DBManager dbManger = new DBManager();
    }

}
