package net.sklcc.wechatsupporter;

import cn.gsdata.index.ApiSdk;
import net.sklcc.wechatsupporter.db.DBServer;
import net.sklcc.wechatsupporter.useless.DataProvider;
import net.sklcc.wechatsupporter.util.TimeUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Hazza on 2016/12/5.
 */
public class GsDataProvider {
    private static Logger logger = Logger.getLogger(DataProvider.class.getName());

    private final static String appId = "aJ2Vx03fS7j4LdxeSrb5";
    private final static String appKey = "zgaWRumyOf586J7z7uiH9E2D4";
    private final static String apiUrl = "http://open.gsdata.cn/api/wx/wxapi/wx_week_readnum";

    private int read_num[];
    private int like_num[];

    private Map<Integer, String> articles;
    private Map<Integer, String> redos;

    private void getArticles(int i) throws SQLException {
        articles = new HashMap<Integer, String>();
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");


        ResultSet rs = sourceDBServer.select("select id, url from wsa_article where to_days(now()) - " +
                "to_days(publish_time) = " + (i+1));
        while (rs.next()) {
            articles.put(rs.getInt(1), rs.getString(2));
        }

        logger.info("There are " + articles.size() + " articles.");
        sourceDBServer.close();
    }

    public void doTask() throws SQLException, ClassNotFoundException {
        ApiSdk apiSdk = ApiSdk.getApiSdk(appId,appKey);
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
//        DBServer destDBServer = new DBServer("proxool.destDb"); //要注释

        for (int i = 6; i >= 0; --i) {
            getArticles(i);

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date(System.currentTimeMillis() - ((i+1) * 24 * 3600 * 1000));
            String columns = "article_id, read_count, like_count, add_time";

            for (Map.Entry<Integer, String> article : articles.entrySet()) {
                Map<String, Object> map = new HashMap<>();
                read_num = new int[7];
                like_num = new int[7];

                map.put("start_time", formatter.format(date));
                map.put("end_time", formatter.format(date));
                map.put("url", article.getValue());
                map.put("page", 0);
                map.put("rows", 10);

                try {
                    String jsonReturned = apiSdk.callInterFace(apiUrl, map);
                    JSONObject jsonObject = new JSONObject(jsonReturned);
                    JSONArray jsonArray = (JSONArray) jsonObject.get("returnData");

                    System.out.println(jsonArray.toString());

                    String title = (String) ((JSONObject) jsonArray.get(0)).get("title");
                    String posttime = (String) ((JSONObject) jsonArray.get(0)).get("posttime");
                    read_num[0] = (int) ((JSONObject) jsonArray.get(0)).get("read_num_1");
                    like_num[0] = (int) ((JSONObject) jsonArray.get(0)).get("like_num_1");
                    read_num[1] = (int) ((JSONObject) jsonArray.get(0)).get("read_num_2");
                    like_num[1] = (int) ((JSONObject) jsonArray.get(0)).get("like_num_2");
                    read_num[2] = (int) ((JSONObject) jsonArray.get(0)).get("read_num_3");
                    like_num[2] = (int) ((JSONObject) jsonArray.get(0)).get("like_num_3");
                    read_num[3] = (int) ((JSONObject) jsonArray.get(0)).get("read_num_4");
                    like_num[3] = (int) ((JSONObject) jsonArray.get(0)).get("like_num_4");
                    read_num[4] = (int) ((JSONObject) jsonArray.get(0)).get("read_num_5");
                    like_num[4] = (int) ((JSONObject) jsonArray.get(0)).get("like_num_5");
                    read_num[5] = (int) ((JSONObject) jsonArray.get(0)).get("read_num_6");
                    like_num[5] = (int) ((JSONObject) jsonArray.get(0)).get("like_num_6");
                    read_num[6] = (int) ((JSONObject) jsonArray.get(0)).get("read_num_7");
                    like_num[6] = (int) ((JSONObject) jsonArray.get(0)).get("like_num_7");

                    logger.info(title + " " + article.getValue() + " " + posttime + "\n" +
                    "Day 1: " + read_num[0] + " " + like_num[0] + "\n" +
                    "Day 2: " + read_num[1] + " " + like_num[1] + "\n" +
                    "Day 3: " + read_num[2] + " " + like_num[2] + "\n" +
                    "Day 4: " + read_num[3] + " " + like_num[3] + "\n" +
                    "Day 5: " + read_num[4] + " " + like_num[4] + "\n" +
                    "Day 6: " + read_num[5] + " " + like_num[5] + "\n" +
                    "Day 7: " + read_num[6] + " " + like_num[6] );

                    HashMap<Integer, Object> params = new HashMap<>();
                    params.put(1, article.getKey());
                    params.put(2, read_num[i]);
                    params.put(3, like_num[i]);
                    params.put(4, TimeUtil.convertMillsToDateString(System.currentTimeMillis()));
                    sourceDBServer.insert("wsa_article_stats", columns, params);
//                    destDBServer.insert("wsa_article_stats", columns, params);  //要注释
                } catch (JSONException e) {
                    /*HashMap<Integer, Object> params = new HashMap<>();
                    params.put(1, article.getKey());
                    params.put(2, 0);
                    params.put(3, 0);
                    params.put(4, TimeUtil.convertMillsToDateString(System.currentTimeMillis()));
                    sourceDBServer.insert("wsa_article_stats", columns, params);*/
                    logger.error(article.getValue() + " doesn't get data.");
                    logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
                }
            }

        }
        sourceDBServer.close();
//        destDBServer.close();   //要注释
    }



    public static void main(String[] args) throws Exception {
        GsDataProvider gsDataProvider = new GsDataProvider();
        gsDataProvider.doTask();
    }
}
