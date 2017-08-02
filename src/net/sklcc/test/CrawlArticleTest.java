package net.sklcc.test;


import cn.gsdata.index.ApiSdk;
import net.sklcc.db.DBServer;
import net.sklcc.util.TimeUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by Hazza on 2016/7/22.
 */
public class CrawlArticleTest {
    private static Logger logger = Logger.getLogger(CrawlArticleTest.class.getName());

    private final static String appId = "aJ2Vx03fS7j4LdxeSrb5";
    private final static String appKey = "zgaWRumyOf586J7z7uiH9E2D4";
    private final static String apiUrl = "http://open.gsdata.cn/api/wx/opensearchapi/content_list";

    private List<String> official_accounts; //保存获取的微信公众号
    private int article_cnt;    //获得的文章数量
    private Map<String,Integer> hashMap;    //判断是否存在重复文章的hashmap

    public  String date;

    public int count = 0;

    /**
     * @discription 通过gsdata提供的接口和jar包获得指定公众号的文章，并保存到数据库
     * @param account  要获取文章的公众号
     * @throws Exception
     */
    private void getArticles(String account) throws Exception {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//        Date yesterday = new Date(System.currentTimeMillis() - (24 * 3600 * 1000));

        ApiSdk apiSdk = ApiSdk.getApiSdk(appId, appKey);
        Map<String, Object> map = new HashMap<>();

        map.put("wx_name",account);
//        map.put("postdate",formatter.format(yesterday));

        //map.put("postdate", "2016-12-03");
        map.put("postdate", date);

        String jsonReturned = apiSdk.callInterFace(apiUrl, map);
        System.out.println(jsonReturned);
        JSONObject jsonObject = new JSONObject(jsonReturned);
        JSONArray jsonArray = (JSONArray) jsonObject.getJSONObject("returnData").get("items");

        article_cnt += jsonArray.length();

        for (int i = 0; i < jsonArray.length(); ++i) {
            String title = (String) ((JSONObject) jsonArray.get(i)).get("title");

            String url = (String) ((JSONObject) jsonArray.get(i)).get("url");
            String publish_time = (String) ((JSONObject) jsonArray.get(i)).get("posttime");
            String summary = (String) ((JSONObject) jsonArray.get(i)).get("content");
            String add_time = (String) ((JSONObject) jsonArray.get(i)).get("add_time");
            String author = (String) ((JSONObject) jsonArray.get(i)).get("author");
            String source_url = (String) ((JSONObject) jsonArray.get(i)).get("sourceurl");
            String copyright = (String) ((JSONObject) jsonArray.get(i)).get("copyright");
//            String ranking = (String) ((JSONObject) jsonArray.get(i)).get("top");
            String ranking = ((JSONObject) jsonArray.get(i)).get("top").toString();


            ++count;
            logger.info("信息: " + account + ' ' + publish_time + ' ' + title + ' ' + summary + ' ' +
                    url + ' ' + add_time + ' ' + ranking + ' ' + source_url + ' ' +
                    author + ' ' + copyright);
        }
    }

    /**
     * @discription 开始抓取操作
     */
    void crawl(){
        official_accounts = new ArrayList<>();
        hashMap = new HashMap<>();

//        int repeat =2;
//        while ((repeat--)>0) {

                try {
                    getArticles("suzhoudaily");
                } catch (Exception e) {
                    logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
                }

//        }

        for (Entry<String, Integer> entry : hashMap.entrySet()) {
            logger.info("key= " + entry.getKey());
        }

        logger.info("There are " + article_cnt + " new articles.");
    }

    public static void main(String[] args) {
        CrawlArticleTest ca = new CrawlArticleTest();
//        ca.crawl();

        String[] dates = TimeUtil.generateFormatData("2017-06-", 1, 30);
        for (int i = 0; i < dates.length; i++) {
            ca.date = dates[i];
            ca.crawl();
        }

        System.out.println(ca.count);

    }
}
