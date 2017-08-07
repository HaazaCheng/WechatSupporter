package net.sklcc.wechatsupporter;

import net.sklcc.wechatsupporter.db.DBServer;
import net.sklcc.wechatsupporter.util.TimeUtil;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Hazza on 2016/7/27.
 */
public class Counter {
    private static Logger logger =Logger.getLogger(Counter.class.getName());

    private ArrayList<String> group_accounts;   //保存每个分组的帐号
    private ArrayList<String> group_articles;   //保存每个分组的文章
    private ArrayList<Integer> groupAccountCount;   //保存每个分组的帐号数量
    private ArrayList<Integer> groupArticleCount;   //保存每个分组的文章数量
    private long allAccountCount ;              //保存所有帐号的数量
    private long allArticleCount;               //保存所有文章的数量

    /**
     * @Description 从数据库读取帐号并按照分组保存
     * @throws SQLException
     */
    private void getAccounts() throws SQLException {
        groupAccountCount = new ArrayList<>();
        allAccountCount = 0;
        int i = 0;

        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        ResultSet rs = sourceDBServer.select("SELECT official_accounts FROM wsa_group");
        while (rs.next()) {
            ArrayList<String> groupList = new ArrayList<>();
            String accounts = rs.getString(1);
            for (String account : accounts.split(","))
            {
                groupList.add('\'' + account + '\'');
            }
            group_accounts.add('(' + groupList.toString().substring(1,groupList.toString().length() - 1) + ')');  //transfer to string
            groupAccountCount.add(groupList.size());
            logger.info("Group" + (++i) +" has " + groupList.size() + " official_accounts.");
            allAccountCount += groupList.size();
        }
        logger.info("Total accounts: " + allAccountCount);
        sourceDBServer.close();
    }

    /**
     * @Description 从数据库读取文章并按照分组保存
     * @throws SQLException
     */
    private void getArticles() throws SQLException {
        groupArticleCount = new ArrayList<>();
        allArticleCount = 0;
        int i = 0;

        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        for (String group : group_accounts) {
            ArrayList<Integer> articleIDs = new ArrayList<>();
            String sql = "SELECT id FROM wsa_article WHERE official_account IN" + group + " AND deleted != 1";
            ResultSet rs = sourceDBServer.select(sql);
            while (rs.next())
            {
                articleIDs.add(Integer.valueOf(rs.getInt(1)));
            }
            group_articles.add('(' + articleIDs.toString().substring(1,articleIDs.toString().length() - 1) + ')');      //transfer to string
            groupArticleCount.add(articleIDs.size());
            logger.info("Group" + (++i) +" has " + articleIDs.size() + " articles.");
            allArticleCount += articleIDs.size();
        }
        logger.info("Total articles: " + allArticleCount);
        sourceDBServer.close();
    }

    /**
     * @Description 计算公众号总数，文章总数，点赞量总数，阅读量总数
     * @throws SQLException
     */
    public void doCount() throws SQLException, ClassNotFoundException {
        group_accounts = new ArrayList<>();
        group_articles = new ArrayList<>();
        getAccounts();
        getArticles();

        long[] read_count = new long[group_accounts.size()];
        long[] like_count = new long[group_accounts.size()];
        long readCounts = 0;
        long likeCounts = 0;
        int i = 0;

        DBServer sourceDBServer = new DBServer("proxool.sourceDb");

        for (String group : group_articles) {

            //select the latest data
            String sql = "SELECT MAX(read_count),MAX(like_count) FROM wsa_article_stats WHERE article_id IN"
                    + group + " GROUP BY article_id";
            ResultSet rs = sourceDBServer.select(sql);
            while (rs.next()) {
                read_count[i] += rs.getInt(1);
                like_count[i] += rs.getInt(2);
            }
            readCounts += read_count[i];
            likeCounts += like_count[i];
            ++i;
            logger.info("Group" + i + "  read_count:" + read_count[i-1] + " like_count:" + like_count[i-1]);
        }
        logger.info("Total read: " + readCounts + "  Total like: " + likeCounts);

        //save to database
        String columns = "group_id, account_count, article_count, read_count, like_count, add_time";
        for (i = 0; i < group_accounts.size(); ++i) {
            HashMap<Integer, Object> params = new HashMap<>();
            params.put(1, i+1);
            params.put(2, groupAccountCount.get(i));
            params.put(3, groupArticleCount.get(i));
            params.put(4, read_count[i]);
            params.put(5, like_count[i]);
            params.put(6, TimeUtil.convertMillsToDateString(System.currentTimeMillis()));

            sourceDBServer.insert("wsa_group_stats", columns, params);
        }
        sourceDBServer.close();
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        long start = System.currentTimeMillis();
        Counter ct = new Counter();
        ct.doCount();
        System.out.println("Time:" + (System.currentTimeMillis() - start));
    }
}
