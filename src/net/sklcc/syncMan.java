package net.sklcc;

import net.sklcc.db.DBServer;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Hazza on 6/23/16.
 */
@SuppressWarnings({"SqlNoDataSourceInspection", "Duplicates"})
public class syncMan {
    static Logger logger = Logger.getLogger(syncMan.class.getName());


    @SuppressWarnings("SqlDialectInspection")
    void doSync() throws SQLException {
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        DBServer destDBServer = new DBServer("proxool.destDb");

        logger.info("Start wsa_article sync...");
        // do wsa_article sync
        ResultSet rs = sourceDBServer.select("select * from wsa_article where to_days(now())" +
                "- to_days(publish_time) = 1");
//        ResultSet rs = sourceDBServer.select("select * from wsa_article where to_days(now())" +
//                "- to_days(publish_time) <= 9");

        ArrayList<ArrayList<String>> rows = new ArrayList<>();

        //noinspection Duplicates
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            for (int i = 1; i <= 13; i += 1) {
                row.add(rs.getString(i));
            }
            rows.add(row);
        }

        for (ArrayList<String> row : rows) {
            String columns = "id, official_account, publish_time, title, summary, url,add_time, ranking, source_url, author, copyright, delete_time, deleted";
            HashMap<Integer, Object> params = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                params.put(i + 1, row.get(i));
            }
            logger.info("params:" + params.toString());

            //noinspection Duplicates
            try {
                destDBServer.insert("wsa_article", columns, params);
            } catch (Exception e) {
                logger.error(e.getClass() + " " + e.getMessage());
            } finally {
                sourceDBServer.close();
                destDBServer.close();
            }
        }

        logger.info("Start wsa_article_stats sync...");
        // do wsa_article_stats sync
        rows.clear();
        rs = sourceDBServer.select("select * from wsa_article_stats where to_days(now())" +
                "- to_days(add_time) = 0");
       /* rs = sourceDBServer.select("select * from wsa_article_stats where to_days(now())" +
                "- to_days(add_time) <= 8");*/
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            for (int i = 1; i <= 7; i++) {
                row.add(rs.getString(i));
            }
            rows.add(row);
        }

        for (ArrayList<String> row : rows) {
            String columns = "id, article_id, read_count, like_count, add_time, delete_time, deleted";
            HashMap<Integer, Object> params = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
               params.put(i + 1, row.get(i));
            }
            logger.info("params:" + params);

            //noinspection Duplicates
            try {
                destDBServer.insert("wsa_article_stats", columns, params);
            } catch (Exception e) {
                logger.error(e.getClass() + " " + e.getMessage());
            } finally {
                sourceDBServer.close();
                destDBServer.close();
            }
        }

        logger.info("Start wsa_group_stats sync...");
        //do wsa_group_stats sync
        rows.clear();
        rs = sourceDBServer.select("SELECT * FROM wsa_group_stats where to_days(now())" +
                "- to_days(add_time) = 0");
//        rs = sourceDBServer.select("SELECT * FROM wsa_group_stats where to_days(now())" +
//                "- to_days(add_time) <= 8");
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            for (int i = 1; i <= 7; i++) {
                row.add(rs.getString(i));
            }
            rows.add(row);
        }

        for (ArrayList<String> row : rows) {
            String columns = "id,group_id, account_count, article_count, read_count, like_count, add_time";
            HashMap<Integer, Object> params = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                params.put(i + 1,row.get(i));
            }
            logger.info("params:" + params);

            //noinspection Duplicates
            try {
               destDBServer.insert("wsa_group_stats", columns, params);
            } catch (Exception e) {
                logger.error(e.getClass() + " " + e.getMessage());
            } finally {
                sourceDBServer.close();
                destDBServer.close();
            }
        }
        

        sourceDBServer.close();
        destDBServer.close();
    }

    public static void main(String[] args) {
        try {
            new syncMan().doSync();
        } catch (Exception e) {
            logger.error(e.getClass() + " " + e.getMessage());
        }
    }
}
