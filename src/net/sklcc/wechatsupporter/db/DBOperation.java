package net.sklcc.wechatsupporter.db;

import java.sql.*;
import java.util.HashMap;

/**
 * Created by Hazza on 2016/8/7.
 */
public class DBOperation {
    private String poolName;
    private Connection con = null;

    public DBOperation(String poolName) {
        this.poolName = poolName;
    }

    public void close() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void open() throws SQLException {
        //after the first closed to prevent the database connection overflow
        close();
        con = DBManager.getDBManger().getConnection(this.poolName);
    }

    /**
     * SQL statement parameter transformation
     * @author hazzacheng
     * @param sql
     * @param params
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private PreparedStatement setPres(String sql, HashMap<Integer, Object> params) throws SQLException, ClassNotFoundException {
        if (sql == null | params.size() < 1) {
            return null;
        }
        PreparedStatement pres = this.con.prepareStatement(sql);
        for (int i = 1; i <= params.size(); i++) {
            if (params.get(i) == null) {
                //pres.setString(i, "");    不能添加"",防止添加null到数据库时被转换为""
                pres.setString(i, null);
            } else if (params.get(i).getClass() == Class.forName("java.lang.String")) {
                pres.setString(i, params.get(i).toString());
            } else if (params.get(i).getClass() == Class.forName("java.lang.Integer")) {
                pres.setInt(i, (Integer) params.get(i));
            } else if (params.get(i).getClass() == Class.forName("java.lang.Long")) {
                pres.setLong(i, (Long) params.get(i));
            } else if (params.get(i).getClass() == Class.forName("java.lang.Double")) {
                pres.setDouble(i, (Double) params.get(i));
            } else if (params.get(i).getClass() == Class.forName("java.lang.Float")) {
                pres.setFloat(i, (Float) params.get(i));
            } else if (params.get(i).getClass() == Class.forName("java.lang.Boolean")) {
                pres.setBoolean(i, (Boolean) params.get(i));
            } else if (params.get(i).getClass() == Class.forName("java.sql.Date")) {
                pres.setDate(i, Date.valueOf(params.get(i).toString()));
            } else {
                return null;
            }
        }

        return pres;
    }

    /**
     * executes the SQL statement
     * @quthor hazzacheng
     * @param sql
     * @return the number of affected rows
     * @throws SQLException
     */
    public int executeUpdate(String sql) throws SQLException {
        this.open();
        Statement sta = this.con.createStatement();
        return sta.executeUpdate(sql);
    }

    /**
     * executes the SQL statement
     * @quthor hazzacheng
     * @param sql
     * @param params
     * @return the number of affected rows
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public int executeUpdate(String sql, HashMap<Integer, Object> params) throws SQLException, ClassNotFoundException {
        this.open();
        PreparedStatement pres = setPres(sql, params);
        if (pres == null) {
            return 0;
        }
        return pres.executeUpdate();
    }

    /**
     * executes the SQL statement
     * @author hazzacheng
     * @param sql
     * @return the result set
     * @throws SQLException
     */
    public ResultSet executeQuery(String sql) throws SQLException {
        this.open();
        Statement sta = this.con.createStatement();
        return sta.executeQuery(sql);
    }

}
