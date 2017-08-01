package net.sklcc.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by Hazza on 2016/8/7.
 */
public class DBServer {
    private DBOperation dbOperation;

    public DBServer(String poolName) {
        dbOperation = new DBOperation(poolName);
    }

    public void close() {
        dbOperation.close();
    }

    public int insert(String sql) throws SQLException {
        return dbOperation.executeUpdate(sql);
    }

    public int insert(String tableName, String columns, HashMap<Integer, Object> params) throws SQLException, ClassNotFoundException {
        String sql = insertSql(tableName, columns);
        return dbOperation.executeUpdate(sql,params);
    }

    /**
     * assenmble insert sql  eg: insert into tableName (column1, column2) values (?,?)
     * @author hazzacheng
     * @param tableName
     * @param columns
     * @return
     */
    private String insertSql(String tableName, String columns) {
        if (tableName == null || columns == null ) {
            return null;
        }
        int count = columns.split(",").length;
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO " + tableName + "(" + columns + ")" + " VALUES(?");
        for (int i = 1; i < count; i++) {
            sb.append(",?");
        }
        sb.append(")");

        return sb.toString();
    }

    public int delete(String sql) throws SQLException {
        return dbOperation.executeUpdate(sql);
    }

    public int delete(String tableName, String condition) throws SQLException {
        String sql = "DELETE FROM " + tableName + " " + condition;
        return dbOperation.executeUpdate(sql);
    }

    public int update(String sql) throws SQLException {
        return dbOperation.executeUpdate(sql);
    }

    public int update(String tableName, String columns, String condition, HashMap<Integer, Object> params) throws SQLException, ClassNotFoundException {
        String sql = updateSql(tableName, columns, condition);
        return dbOperation.executeUpdate(sql, params);
    }

    /**
     * assemble update sql eg: update tableName set column1=?,column2=? condition
     * @author hazzacheng
     * @param tableName
     * @param columns
     * @param condition
     * @return
     */
    public String updateSql(String tableName, String columns, String condition) {
        if (tableName == null || columns == null) {
            return null;
        }
        String[] column = columns.split(",");
        StringBuffer sb = new StringBuffer();
        sb.append("UPDATE " + tableName + " SET ");
        sb.append(column[0] + "=?");
        for (int i = 1; i < column.length; i++) {
            sb.append(", " + column[i] + "=?");
        }
        sb.append(" " + condition);

        return sb.toString();
    }

    public ResultSet select(String sql) throws SQLException {
        return dbOperation.executeQuery(sql);
    }

    public ResultSet select(String tableName, String columns, String condition) throws SQLException {
        String sql = "SELECT " + columns + " FROM " + tableName + " " +condition;
        return dbOperation.executeQuery(sql);
    }


}
