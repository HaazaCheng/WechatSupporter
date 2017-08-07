package net.sklcc.recover;

import cn.gsdata.index.ApiSdk;
import net.sklcc.CrawlArticle;
import net.sklcc.db.DBServer;
import net.sklcc.util.TimeUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hazza on 2016/11/24.
 * 恢复指定时间发布的文章的阅读量，点赞量
 */
public class DateDataRecover {
    private static Logger logger = Logger.getLogger(CrawlArticle.class.getName());

    private final static String appId = "aJ2Vx03fS7j4LdxeSrb5";
    private final static String appKey = "zgaWRumyOf586J7z7uiH9E2D4";
    private final static String apiUrl = "http://open.gsdata.cn/api/wx/wxapi/wx_week_readnum";

    private int[] read_num;
    private int[] like_num;

    private Map<Integer, String> articles;
    /*private static String[] dates = {"2017-01-29", "2017-01-30", "2017-01-31",
            "2017-02-01", "2017-02-02", "2017-02-03", "2017-02-04", "2017-02-05", "2017-02-06", "2017-02-07", "2017-02-08",
            "2017-02-09", "2017-02-10", "2017-02-11", "2017-02-12", "2017-02-13", "2017-02-14", "2017-02-15", "2017-02-16",
            "2017-02-17", "2017-02-18", "2017-02-19", "2017-02-20", "2017-02-21", "2017-02-22","2017-02-23"};*/
   private static String[] dates = TimeUtil.generateFormatData(2017, 6, 1, 30);
//   private static String[] dates = {"2017-06-07"};

    /**
     * 获得指定日期的所有文章保存到数据库中
     */
    private void getArticles() {
        CrawlArticle ca = new CrawlArticle();

        for (int i = 0; i < dates.length; i++) {
            logger.info("Start to crawl articles in " + dates[i]);
            ca.date = dates[i];
            ca.crawl();
            logger.info("Finish crawling articles in " + dates[i]);
        }
    }

    /**
     * 获得符合日期当天发布的文章
     * @param date
     * @throws Exception
     */
    private void getOneDayArticles(String date) throws Exception {
        articles = new HashMap<Integer, String>();

        int intervalDays = TimeUtil.getIntervalDays(TimeUtil.convertStringToDate(date), new Date());//通过相差天数来搜索对应的时间
        String sql = "select id, url, publish_time from wsa_article where to_days(now()) - to_days(publish_time) = " + intervalDays;
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");

        ResultSet rs = sourceDBServer.select(sql);
        while (rs.next()) {
            articles.put(rs.getInt(1), rs.getString(2));
        }

        logger.info("There are " + articles.size() + " articles in " + date);

        sourceDBServer.close();
    }

    /**
     * 获得一天所有文章的数据并保存到数据库
     * @param date
     * @throws Exception
     */
    private void getOneDayData(String date) throws Exception {
        ApiSdk apiSdk = ApiSdk.getApiSdk(appId,appKey);
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
//        DBServer destDBServer = new DBServer("proxool.destDb");
        String columns = "article_id, read_count, like_count, add_time";
        getOneDayArticles(date);

        for (Map.Entry<Integer, String> article : articles.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            read_num = new int[7];
            like_num = new int[7];

            map.put("start_time", date);
            map.put("end_time", date);
            map.put("url", article.getValue());
            map.put("page", 0);
            map.put("rows", 10);

            String jsonReturned = apiSdk.callInterFace(apiUrl, map);
            JSONObject jsonObject = new JSONObject(jsonReturned);
            JSONArray jsonArray = (JSONArray) jsonObject.get("returnData");

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


            for (int i = 0; i < 7; i++) {
                /*if (TimeUtil.getIntervalDays(TimeUtil.addDay(TimeUtil.convertStringToDate(date), i), new Date()) <= 0) {
                    break;
                }*/

                HashMap<Integer, Object> params = new HashMap<>();
                String add_time = TimeUtil.convertDateToDateString(TimeUtil.addDay(TimeUtil.convertStringToDate(date), i + 1));

                if (add_time.equals("2017-07-12 00:00:00")) {
                    break;
                }

                /*if (add_time.equals("2017-02-23 00:00:00") || add_time.equals("2017-02-24 00:00:00")) {
                    params.put(1, article.getKey());
                    params.put(2, read_num[i]);
                    params.put(3, like_num[i]);
                    params.put(4, add_time);
                    sourceDBServer.insert("wsa_article_stats", columns, params);
                }*/

                params.put(1, article.getKey());
                params.put(2, read_num[i]);
                params.put(3, like_num[i]);
                params.put(4, add_time);
                sourceDBServer.insert("wsa_article_stats", columns, params);
//                destDBServer.insert("wsa_article_stats", columns, params);
            }

        }
        sourceDBServer.close();
//        destDBServer.close();
    }

    /**
     * 获得某一篇文章的阅读量点赞量
     * @param url
     */
    @SuppressWarnings("unused")
    private void getOneArticleData(String url) {
        ApiSdk apiSdk = ApiSdk.getApiSdk(appId,appKey);
        Map<String, Object> map = new HashMap<>();

        map.put("start_time", "2016-10-13");
        map.put("end_time", "2016-10-13");
        map.put("url", url);
        map.put("page", 0);
        map.put("rows", 10);

        String jsonReturned = apiSdk.callInterFace(apiUrl, map);
        JSONObject jsonObject = new JSONObject(jsonReturned);

        JSONArray jsonArray = (JSONArray) jsonObject.get("returnData");

        String title = (String) ((JSONObject) jsonArray.get(0)).get("title");
        String posttime = (String) ((JSONObject) jsonArray.get(0)).get("posttime");
        int read_num_1 = (int) ((JSONObject) jsonArray.get(0)).get("read_num_1");
        int like_num_1 = (int) ((JSONObject) jsonArray.get(0)).get("like_num_1");
        int read_num_2 = (int) ((JSONObject) jsonArray.get(0)).get("read_num_2");
        int like_num_2 = (int) ((JSONObject) jsonArray.get(0)).get("like_num_2");
        int read_num_3 = (int) ((JSONObject) jsonArray.get(0)).get("read_num_3");
        int like_num_3 = (int) ((JSONObject) jsonArray.get(0)).get("like_num_3");
        int read_num_4 = (int) ((JSONObject) jsonArray.get(0)).get("read_num_4");
        int like_num_4 = (int) ((JSONObject) jsonArray.get(0)).get("like_num_4");
        int read_num_5 = (int) ((JSONObject) jsonArray.get(0)).get("read_num_5");
        int like_num_5 = (int) ((JSONObject) jsonArray.get(0)).get("like_num_5");
        int read_num_6 = (int) ((JSONObject) jsonArray.get(0)).get("read_num_6");
        int like_num_6 = (int) ((JSONObject) jsonArray.get(0)).get("like_num_6");
        int read_num_7 = (int) ((JSONObject) jsonArray.get(0)).get("read_num_7");
        int like_num_7 = (int) ((JSONObject) jsonArray.get(0)).get("like_num_7");

        System.out.println(title + " " + posttime +" ");
        System.out.println("Day 1: " + read_num_1 + " " + like_num_1 +" ");
        System.out.println("Day 2: " + read_num_2 + " " + like_num_2 +" ");
        System.out.println("Day 3: " + read_num_3 + " " + like_num_3 +" ");
        System.out.println("Day 4: " + read_num_4 + " " + like_num_4 +" ");
        System.out.println("Day 5: " + read_num_5 + " " + like_num_5 +" ");
        System.out.println("Day 6: " + read_num_6 + " " + like_num_6 +" ");
        System.out.println("Day 7: " + read_num_7 + " " + like_num_7 +" ");

       /* logger.info(title + " " + posttime +" ");
        logger.info("Day 1: " + read_num[0] + " " + read_num[0] +" ");
        logger.info("Day 2: " + read_num[1] + " " + read_num[1] +" ");
        logger.info("Day 3: " + read_num[2] + " " + read_num[2] +" ");
        logger.info("Day 4: " + read_num[3] + " " + read_num[3] +" ");
        logger.info("Day 5: " + read_num[4] + " " + read_num[4] +" ");
        logger.info("Day 6: " + read_num[5] + " " + read_num[5] +" ");
        logger.info("Day 7: " + read_num[6] + " " + read_num[6] +" ");
*/
    }

    public void doTask() {
        /*logger.info("Start to crawl articles.");
        getArticles();  //注意注释
        logger.info("Finishing crawling articles.");*/
        for (String date : dates) {
            try {
                logger.info("Start to recover data in " + date);
                getOneDayData(date);
                logger.info("Finish recovering data in " + date);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.info("Finish recovering data.");
    }

    public static void main(String[] args) {
        DateDataRecover dataRecover = new DateDataRecover();
//        dataRecover.getOneArticleData("http://mp.weixin.qq.com/s?__biz=MzA3OTE0NDIzMA==&mid=2651858354&idx=1&sn=65632bda1e71914681d2807caf1bb6d7&chksm=845371fbb324f8edbc95bf58237ded8d0c7c01c86bcb8669a51e021dfd05fbe0a66cbaa79b26&scene=4#wechat_redirect");
        /*try {
            dataRecover.getOneDayData("2016-10-22");
        } catch (Exception e) {
            e.printStackTrace();
        }*/
/*        try {
            dataRecover.getArticles("2016-11-15");
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        dataRecover.doTask();
    }
}
