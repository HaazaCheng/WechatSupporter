package net.sklcc;


import cn.gsdata.index.ApiSdk;
import net.sklcc.db.DBServer;
import net.sklcc.util.GroupUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;

/**
 * Created by Hazza on 2016/7/22.
 */
public class CrawlArticle {
    private static Logger logger = Logger.getLogger(CrawlArticle.class.getName());

    private final static String appId = "aJ2Vx03fS7j4LdxeSrb5";
    private final static String appKey = "zgaWRumyOf586J7z7uiH9E2D4";
    private final static String apiUrl = "http://open.gsdata.cn/api/wx/opensearchapi/content_list";

    private List<String> official_accounts; //保存获取的微信公众号
    private int article_cnt;    //获得的文章数量
    private int duplicated;     //重复的文章数量
    private Map<String,Integer> hashMap;    //判断是否存在重复文章的hashmap

    public  String date;
    private static int[] ignoreGroup = {5, 11};

    /**
     * @discription 从数据库获得要爬取的微信公众号
     * @throws Exception
     */
    private void getAccounts() throws Exception {
        //获得不抓取的公众号
        HashSet<String> ignoreAccounts = GroupUtil.getGroupAccounts(ignoreGroup);

        DBServer sourceDBServer = new DBServer("proxool.sourceDb");

        String sql = "select official_account from wsa_official_account";
        ResultSet rs = sourceDBServer.select(sql);
        while (rs.next()) {
            //需要停止抓取某些公众号的情况
            if (!ignoreAccounts.contains(rs.getString(1))) {
                official_accounts.add(rs.getString(1));
            }
            //不需要停止抓取某些公众号的情况
//            official_accounts.add(rs.getString(1));
        }

        logger.info(official_accounts.size() + " official_accounts.");
        sourceDBServer.close();
    }

    /**
     * @discription 通过gsdata提供的接口和jar包获得指定公众号的文章，并保存到数据库
     * @param account  要获取文章的公众号
     * @throws Exception
     */
    private void getArticles(String account) throws Exception {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date yesterday = new Date(System.currentTimeMillis() - (24 * 3600 * 1000 * 2));

        ApiSdk apiSdk = ApiSdk.getApiSdk(appId,appKey);
        Map<String, Object> map = new HashMap<>();

        map.put("wx_name",account);
        map.put("postdate",formatter.format(yesterday));

//        map.put("postdate", "2017-05-07");
//        map.put("postdate", date);  //要注释

        String jsonReturned = apiSdk.callInterFace(apiUrl, map);
        JSONObject jsonObject = new JSONObject(jsonReturned);
        JSONArray jsonArray = (JSONArray) jsonObject.getJSONObject("returnData").get("items");

        article_cnt += jsonArray.length();

        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
//        DBServer destDBServer = new DBServer("proxool.destDb"); //要注释
        String columns = "official_account, publish_time, title, summary, url, "
                + "add_time, ranking, source_url, author, copyright";
        for (int i = 0; i < jsonArray.length(); ++i) {
            String title = (String) ((JSONObject) jsonArray.get(i)).get("title");
            String toHash = account + title;
            if (hashMap.containsKey(toHash)) {
                ++duplicated;
                --article_cnt;
                continue;
            } else {
                hashMap.put(toHash,1);
            }

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
//            destDBServer.insert("wsa_article", columns, params);    //要注释

            logger.info("Infomation: " + account + ' ' + publish_time + ' ' + title + ' ' + summary + ' ' +
                    url + ' ' + add_time + ' ' + ranking + ' ' + source_url + ' ' +
                    author + ' ' + copyright);
        }
        sourceDBServer.close();
//        destDBServer.close();   //要注释
    }

    /**
     * @discription 开始抓取操作
     */
    public void crawl(){
        official_accounts = new ArrayList<>();
        hashMap = new HashMap<>();

        try {
            getAccounts();
        } catch (Exception e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        }

        int repeat =1;
        while ((repeat--)>0) {
            for (String accout : official_accounts) {
                try {
                    getArticles(accout);
                } catch (Exception e) {
                    logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
                }
            }
        }

        for (Entry<String, Integer> entry : hashMap.entrySet()) {
            logger.info("key= " + entry.getKey());
        }

        logger.info("Duplicates : " + duplicated);
        logger.info("There are " + article_cnt + " new articles.");
    }

    public static void main(String[] args) {
        CrawlArticle ca = new CrawlArticle();
        ca.crawl();

        /*String[] dates = {"2017-07-03", "2017-07-04", "2017-07-05", "2017-07-06", "2017-07-07", "2017-07-08"
                ,"2017-07-09", "2017-07-10"};
        for (int i = 0; i < dates.length; i++) {
            ca.date = dates[i];
            ca.crawl();
        }*/

    }
}
