package net.sklcc.wechatsupporter.db;

import net.sklcc.wechatsupporter.util.ClassUtil;

/**
 * Created by Hazza on 2016/10/9.
 */
public class DBPool {
    private String poolPath;

    private DBPool() {
    }

    public static DBPool getDBPool() {
        return DBPoolDao.dbPool;
    }

    /**
     * static internal class to achieve singleton pattern
     * @author hazzacheng
     */
    private static class DBPoolDao {
        private static DBPool dbPool = new DBPool();
    }

    public String getPoolPath() {
        if (poolPath == null) {
            //if the poolPath is empty, the value is assigned as the default value
            return ClassUtil.getClassRootPath(DBPool.class) + "proxool.xml";

        }
        return poolPath;
    }

    public void setPoolPath(String poolPath) {
        this.poolPath = poolPath;
    }

    public static void main(String[] args) {
        DBPoolDao dbPoolDao = new DBPoolDao();

    }
}