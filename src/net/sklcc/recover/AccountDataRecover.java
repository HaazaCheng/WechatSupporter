package net.sklcc.recover;

import cn.gsdata.index.ApiSdk;
import net.sklcc.CrawlArticle;
import net.sklcc.db.DBServer;
import net.sklcc.util.ExcelUtil;
import net.sklcc.util.TimeUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Hazza on 2017/1/14.
 * 恢复指定帐号在指定日期发布的文章后七天的阅读量和点赞量
 */
public class AccountDataRecover {
    private static Logger logger = Logger.getLogger(CrawlArticle.class.getName());

    DBServer sourceDBServer;
    DBServer destDBServer;

    private final static String appId = "aJ2Vx03fS7j4LdxeSrb5";
    private final static String appKey = "zgaWRumyOf586J7z7uiH9E2D4";
    private final static String apiUrl1 = "http://open.gsdata.cn/api/wx/opensearchapi/content_list";
    private final static String apiUrl2 = "http://open.gsdata.cn/api/wx/wxapi/wx_week_readnum";
    private ApiSdk apiSdk;

    private String today;

    private final String[] accounts;
    private final static String[] dates = {"2017-07-01","2017-07-02","2017-07-03","2017-07-04","2017-07-05","2017-07-06","2017-07-07",
            "2017-07-08","2017-07-09","2017-07-10","2017-07-11","2017-07-12"};

    AccountDataRecover(String today) {
        List<String> list = ExcelUtil.readAccountsFromExcel("C:\\Users\\Krystal\\Desktop\\accounts.xls");
        accounts = new String[list.size()];
        list.toArray(accounts);
        this.today = today;
        apiSdk = ApiSdk.getApiSdk(appId,appKey);
    }

    private void getAllAcountsData() throws Exception {
        logger.info("Start to get data.");
        logger.info("There are " + accounts.length + " accounts");
        int count = 0;
        for (String account: accounts) {
            logger.info(account + " is the " + (++count) + "th");
            getAllDateArticlesData(account);
        }
        logger.info("Finishing getting data.");
    }

    /**
     * 获得一个公众号在所有指定日期内的所有文章及它们七天的阅读量点赞量
     * @param account
     */
    private void getAllDateArticlesData(String account) {
        for (String date : dates) {
            try {
                List<String> articles = getOneDayArticles(account, date);
                getOneDayArticlesData(articles, date);
            } catch (Exception e) {
                logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
            }

        }
    }

    /**
     * 获得一个公众号在某一天的所有文章
     * @param account
     * @param date
     * @return 文章的名字
     * @throws Exception
     */
    private List<String> getOneDayArticles(String account, String date) throws Exception {
        Map<String, Object> map = new HashMap<>();
        List<String> articles = new ArrayList<String>();

        map.put("wx_name", account);
        map.put("postdate", date);

        String jsonReturned = apiSdk.callInterFace(apiUrl1, map);
        JSONObject jsonObject = new JSONObject(jsonReturned);
        JSONArray jsonArray = (JSONArray) jsonObject.getJSONObject("returnData").get("items");

        logger.info(date + ": " + account + " has " + jsonArray.length() + " articles.");

        sourceDBServer = new DBServer("proxool.sourceDb");
        destDBServer = new DBServer("proxool.destDb");
        String columns = "official_account, publish_time, title, summary, url, "
                + "add_time, ranking, source_url, author, copyright";
        for (int i = 0; i < jsonArray.length(); ++i) {
            String title = (String) ((JSONObject) jsonArray.get(i)).get("title");
            String url = (String) ((JSONObject) jsonArray.get(i)).get("url");
            String publish_time = (String) ((JSONObject) jsonArray.get(i)).get("posttime");
            String summary = (String) ((JSONObject) jsonArray.get(i)).get("content");
            String add_time = (String) ((JSONObject) jsonArray.get(i)).get("add_time");
            String author = (String) ((JSONObject) jsonArray.get(i)).get("author");
            String source_url = (String) ((JSONObject) jsonArray.get(i)).get("sourceurl");
            String copyright = (String) ((JSONObject) jsonArray.get(i)).get("copyright");
            String ranking = ((JSONObject) jsonArray.get(i)).get("top").toString();

            HashMap<Integer, Object> params = new HashMap<>();
            params.put(1, account);
            params.put(2, publish_time);
            params.put(3, title);
            params.put(4, summary);
            params.put(5, url);
            params.put(6, add_time);
            params.put(7, ranking);
            params.put(8, source_url);
            params.put(9, author);
            params.put(10, copyright);

            sourceDBServer.insert("wsa_article", columns, params);
            destDBServer.insert("wsa_article", columns, params);

            logger.info(account + ' ' + publish_time + ' ' + title + ' ' + summary + ' ' +
                    url + ' ' + add_time + ' ' + ranking + ' ' + source_url + ' ' +
                    author + ' ' + copyright);

            articles.add(title);
        }
        sourceDBServer.close();
        destDBServer.close();

        return articles;
    }

    /**
     * 获得某一天指定日期所有文章的阅读量点赞量并保存到数据库中
     * @param articlesTitle
     * @param date
     * @throws Exception
     */
    private void getOneDayArticlesData(List<String> articlesTitle, String date) throws Exception {
        Map<Integer, String> articles = new HashMap<Integer, String>(getArticlesIdUrl(articlesTitle));
        sourceDBServer = new DBServer("proxool.sourceDb");
        destDBServer = new DBServer("proxool.destDb");
        String columns = "article_id, read_count, like_count, add_time";

        for (Map.Entry<Integer, String> article : articles.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            int[] read_num = new int[7];
            int[] like_num = new int[7];

            map.put("start_time", date);
            map.put("end_time", date);
            map.put("url", article.getValue());
            map.put("page", 0);
            map.put("rows", 10);

            try {
                String jsonReturned = apiSdk.callInterFace(apiUrl2, map);
                JSONObject jsonObject = new JSONObject(jsonReturned);
                System.out.println(jsonObject.toString());
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
                    HashMap<Integer, Object> params = new HashMap<>();
                    String add_time = TimeUtil.convertDateToDateString(TimeUtil.addDay(TimeUtil.convertStringToDate(date), i + 1));

                    if (add_time.equals(today)) {
                        break;
                    }

                    params.put(1, article.getKey());
                    params.put(2, read_num[i]);
                    params.put(3, like_num[i]);
                    params.put(4, add_time);
                    sourceDBServer.insert("wsa_article_stats", columns, params);
                    destDBServer.insert("wsa_article_stats", columns, params);
                }
            } catch (JSONException e) {
                logger.info(article.getValue() + " don't have the nums of read and like");
                logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
            }
        }
        sourceDBServer.close();
        destDBServer.close();
    }


    /**
     * 获得文章的在数据库中的id和url
     * @param articles
     * @return 保存文章id和url的map
     * @throws SQLException
     */
    private Map<Integer, String> getArticlesIdUrl(List<String> articles) throws SQLException {
        Map<Integer, String> articlesId = new HashMap<Integer, String>();

        sourceDBServer = new DBServer("proxool.sourceDb");

        for (String article: articles) {
            ResultSet rs = sourceDBServer.select("select id, url from wsa_article where title = " + "'" + article + "'");
            while (rs.next()) {
                articlesId.put(rs.getInt(1), rs.getString(2));
            }

        }
        sourceDBServer.close();

        return articlesId;
    }

    public static void main(String[] args) throws Exception {
        AccountDataRecover accountDataRecover = new AccountDataRecover("2017-07-14 00:00:00");
//        accountDataRecover.getAllDateArticlesData("suzhoubang");
        accountDataRecover.getAllAcountsData();

    }
}
