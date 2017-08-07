package net.sklcc.wechatsupporter.recover;

import cn.gsdata.index.ApiSdk;
import net.sklcc.wechatsupporter.CrawlArticle;
import net.sklcc.wechatsupporter.db.DBServer;
import net.sklcc.wechatsupporter.object.ArticleInfos;
import net.sklcc.wechatsupporter.util.TimeUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by hazza on 8/7/17.
 */
public class AutoRecover {
    private final static String appId = "aJ2Vx03fS7j4LdxeSrb5";
    private final static String appKey = "zgaWRumyOf586J7z7uiH9E2D4";
    private final static String crawlArtilceApiUrl = "http://open.gsdata.cn/api/wx/opensearchapi/content_list";

    private static Logger logger = Logger.getLogger(CrawlArticle.class.getName());
    private final String[] dates;

    private int absentArticlesCounter;

    /**
     * 构造函数
     *
     * @param year　年份
     * @param month　月份
     */
    public AutoRecover(int year, int month) {
        dates = TimeUtil.generateFormatData(year, month);
        absentArticlesCounter = 0;
    }

    /**
     * 根据帐号和日期，读取已经保存在数据库里的文章标题，并加入一个Set里返回
     *
     * @param account　帐号名
     * @param date　日期
     * @return　保存文章标题的Set
     */
    private Set<String> getExistArticlesTitle(String account, String date) {
        Set<String> articles = new HashSet<>();
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        String sql = "select title from wsa_article where official_account = '" + account +
                "'" + "and publish_time like '" + date +"%'";
        try (ResultSet rs = sourceDBServer.select(sql)){
            while (rs.next()) articles.add(rs.getString(1));
        } catch (SQLException e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        } finally {
            sourceDBServer.close();
        }

        return articles;
    }

    /**
     * 从gsdata获取该帐号在指定日期发表的文章，如果在数据库中不存在的话，存入本地数据库，并返回缺失的对象化文章
     *
     * @param existArticles　已存在数据库的文章
     * @param account　账号
     * @param date　指定日期
     * @return　缺失的文章对象
     */
    private List<ArticleInfos> getAbsentArticles(Set<String> existArticles, String account, String date) {
        List<ArticleInfos> absentArticles = new ArrayList<>();

        ApiSdk apiSdk = ApiSdk.getApiSdk(appId,appKey);
        Map<String, Object> map = new HashMap<>();
        map.put("wx_name",account);
        map.put("postdate",date);
        String jsonReturned = apiSdk.callInterFace(crawlArtilceApiUrl, map);
        JSONObject jsonObject = new JSONObject(jsonReturned);
        JSONArray jsonArray = (JSONArray) jsonObject.getJSONObject("returnData").get("items");
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        String columns = "official_account, publish_time, title, summary, url, "
                + "add_time, ranking, source_url, author, copyright";
        for (int i = 0; i < jsonArray.length(); ++i) {
            ArticleInfos article = new ArticleInfos();
            article.setAccount(account);
            article.setPublish_time((String) ((JSONObject) jsonArray.get(i)).get("posttime"));
            article.setTitle((String) ((JSONObject) jsonArray.get(i)).get("title"));
            article.setSummary((String) ((JSONObject) jsonArray.get(i)).get("content"));
            article.setUrl((String) ((JSONObject) jsonArray.get(i)).get("url"));
            article.setAdd_time((String) ((JSONObject) jsonArray.get(i)).get("add_time"));
            article.setRanking(((JSONObject) jsonArray.get(i)).get("top").toString());
            article.setSource_url((String) ((JSONObject) jsonArray.get(i)).get("sourceurl"));
            article.setAuthor((String)((JSONObject) jsonArray.get(i)).get("author"));
            article.setCopyright((String) ((JSONObject) jsonArray.get(i)).get("copyright"));
            if (!existArticles.contains(article.getTitle())) {
                HashMap<Integer, Object> params = new HashMap<>();
                params.put(1, article.getAccount());
                params.put(2, article.getPublish_time());
                params.put(3, article.getTitle());
                params.put(4, article.getSummary());
                params.put(5, article.getUrl());
                params.put(6, article.getAdd_time());
                params.put(7, article.getRanking());
                params.put(8, article.getSource_url());
                params.put(9, article.getAuthor());
                params.put(10, article.getCopyright());
                try {
                    sourceDBServer.insert("test_wsa_article", columns, params);
                    absentArticles.add(article);
                    ++absentArticlesCounter;
                    logger.info("LOSE: " + article);
                } catch (Exception e) {
                    logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
                }
            }
        }
        sourceDBServer.close();

        return absentArticles;
    }

    /**
     * 通过查询本地数据库文章的ｉd,将文章保存到远程数据库里,与本地数据库所有信息完全一致
     *
     * @param articles　文章列表　
     */
    private void saveToRemoteDB(List<ArticleInfos> articles) {
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        DBServer destDBServer = new DBServer("proxool.destDb");
        String columns = "id, official_account, publish_time, title, summary, url, "
                + "add_time, ranking, source_url, author, copyright";


        for (ArticleInfos article: articles) {
            //因为要保持本地数据库和远程数据库文章id一致，所以要先从本地数据库读出文章id,一并插入远程数据库中．
            String sql = "select id from test_wsa_article " +
                    "where official_account = '" + article.getAccount() +
                    "' and title = '" + article.getTitle() + "'";
            try (ResultSet rs = sourceDBServer.select(sql)) {
                if (rs.next()) {
                    article.setId(String.valueOf(rs.getInt(1)));
                    HashMap<Integer, Object> params = new HashMap<>();
                    params.put(1, article.getId());
                    params.put(2, article.getAccount());
                    params.put(3, article.getPublish_time());
                    params.put(4, article.getTitle());
                    params.put(5, article.getSummary());
                    params.put(6, article.getUrl());
                    params.put(7, article.getAdd_time());
                    params.put(8, article.getRanking());
                    params.put(9, article.getSource_url());
                    params.put(10, article.getAuthor());
                    params.put(11, article.getCopyright());
                    destDBServer.insert("test_wsa_article", columns, params);
                } else {
                    logger.info("Unable to insert into remote db: " + article);
                }
            } catch (Exception e) {
                logger.info("Unable to insert into remote db: " + article);
                logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
            }
        }

        sourceDBServer.close();
        destDBServer.close();
    }

    private void doRecover() {
        for (String date: dates) {
            Set<String> existArticlesTitle = getExistArticlesTitle("suzhoudaily", date);
            List<ArticleInfos> absentArticles = getAbsentArticles(existArticlesTitle, "suzhoudaily", date);
            saveToRemoteDB(absentArticles);
        }
    }


    public static void main(String[] args) {
        AutoRecover autoRecover = new AutoRecover(2017, 6);
        autoRecover.doRecover();
    }
}
