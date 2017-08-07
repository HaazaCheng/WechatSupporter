package net.sklcc.wechatsupporter.util;

import net.sklcc.wechatsupporter.db.DBServer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

/**
 * Created by Hazza on 2017/2/22.
 */
public class GroupUtil {
    private GroupUtil() {}

    /**
     * 从数据库中读出指定分组下的所有用户名
     * @param groups
     * @return
     */
    public static HashSet<String> getGroupAccounts(int[] groups) {
        HashSet<String> accounts = new HashSet<String>();
        DBServer sourceServer = new DBServer("proxool.sourceDb");

        for (int i = 0; i < groups.length; i++) {
            String sql = "select official_accounts from wsa_group where id = " + groups[i];
            try {
                ResultSet rs = sourceServer.select(sql);
                while (rs.next()) {
                    for (String account: rs.getString(1).split(",")) {
                        accounts.add(account);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        sourceServer.close();

        return accounts;
    }

    public static void main(String[] args) {
        int[] groups = {5, 11};
        HashSet<String> accounts = GroupUtil.getGroupAccounts(groups);
        for (String account: accounts) {
            System.out.println(account + " ");
        }

        System.out.println(accounts.contains("CCB-I-WANT"));
        System.out.println(accounts.contains("suzhouditie"));
    }
}
