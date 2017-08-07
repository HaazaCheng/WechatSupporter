package net.sklcc.wechatsupporter.recover;

import cn.gsdata.index.ApiSdk;
import net.sklcc.wechatsupporter.CrawlArticle;
import net.sklcc.wechatsupporter.db.DBServer;
import net.sklcc.wechatsupporter.object.ArticleInfos;
import net.sklcc.wechatsupporter.util.GroupUtil;
import net.sklcc.wechatsupporter.util.TimeUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

/**
 * Created by hazza on 8/7/17.
 */
public class AutoRecover {
    private final static String appId = "aJ2Vx03fS7j4LdxeSrb5";
    private final static String appKey = "zgaWRumyOf586J7z7uiH9E2D4";
    private final static String crawlArtilceApiUrl = "http://open.gsdata.cn/api/wx/opensearchapi/content_list";
    private final static String readsAndLikesApiUrl = "http://open.gsdata.cn/api/wx/wxapi/wx_week_readnum";

    private static Logger logger = Logger.getLogger(CrawlArticle.class.getName());
    private static final int[] ignoreGroup = {5, 11};
    private final String[] dates;
    private final List<String> accounts;
    private int absentArticlesCounter;

    /**
     * 构造函数，在其中初始化了日期和账号
     *
     * @param year　年份
     * @param month　月份
     */
    public AutoRecover(int year, int month) {
        dates = TimeUtil.generateFormatData(year, month);
        accounts = getAccounts();
        for (String a: accounts) {
            System.out.println(a);
        }
        absentArticlesCounter = 0;
    }

    /**
     * 从数据库中读取需要恢复的公众号
     *
     * @return
     */
    private List<String> getAccounts() {
        //获得不抓取的公众号
        Set<String> ignoreAccounts = GroupUtil.getGroupAccounts(ignoreGroup);
        List<String> accounts = new ArrayList<>();
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");

        String sql = "select official_account from wsa_official_account";
        try (ResultSet rs = sourceDBServer.select(sql)) {
            while (rs.next()) {
                if (!ignoreAccounts.contains(rs.getString(1))) {
                    accounts.add(rs.getString(1));
                }
            }
        } catch (Exception e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        } finally {
            sourceDBServer.close();
        }

        logger.info(accounts.size() + " official_accounts need to be checked to recover.");
        return accounts;
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
                    sourceDBServer.insert("wsa_article", columns, params);
                    absentArticles.add(article);
                    ++absentArticlesCounter;
                    logger.info("LOSE: " + account + "-" + article);
                } catch (Exception e) {
                    logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
                }
            }
        }
        sourceDBServer.close();
        if (absentArticles.isEmpty())
            logger.info(account + " lose " + absentArticles.size() + " articles in " + date);

        return absentArticles;
    }

    /**
     * 查找文章列表里的文章在本地数据库里的id，并插入文章对象内．
     *
     * @param articles 文章列表
     */
    private void setArticleId(List<ArticleInfos> articles) {
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        for (ArticleInfos article: articles) {
            //因为要保持本地数据库和远程数据库文章id一致，所以要先从本地数据库读出文章id,插入文章对象里．
            String sql = "select id from wsa_article " +
                    "where official_account = '" + article.getAccount() +
                    "' and title = '" + article.getTitle() + "'";
            try (ResultSet rs = sourceDBServer.select(sql)) {
                if (rs.next()) article.setId(String.valueOf(rs.getInt(1)));
                else logger.info("Unable to set id: " + article);
            } catch (Exception e) {
                logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
            }
        }
        sourceDBServer.close();
    }

    /**
     * 将文章保存到远程数据库里,与本地数据库所有信息完全一致
     *
     * @param articles　文章列表　
     */
    private void saveToRemoteDB(List<ArticleInfos> articles) {
        DBServer destDBServer = new DBServer("proxool.destDb");
        String columns = "id, official_account, publish_time, title, summary, url, "
                + "add_time, ranking, source_url, author, copyright";

        for (ArticleInfos article: articles) {
            try {
                if (!article.getId().equals("")) {
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
                    destDBServer.insert("wsa_article", columns, params);
                } else {
                    logger.info("Unable to insert into remote db: " + article);
                }
            } catch (Exception e) {
                logger.info("Unable to insert into remote db: " + article);
                logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
            }
        }

        destDBServer.close();
    }

    /**
     * 获取指定文章列表里在指定日期开始的七天阅读量和点赞量，并存入本地和远程数据库中
     *
     * @param articles　制定文章列表
     * @param date　指定日期
     */
    private void getReadsAndLikes(List<ArticleInfos> articles, String date) {
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        DBServer destDBServer = new DBServer("proxool.destDb");
        String columns = "article_id, read_count, like_count, add_time";

        for (ArticleInfos article: articles) {
            int[] read_num = new int[7];
            int[] like_num = new int[7];

            Map<String, Object> map = new HashMap<>();
            map.put("start_time", date);
            map.put("end_time", date);
            map.put("url", article.getUrl());
            map.put("page", 0);
            map.put("rows", 10);

            ApiSdk apiSdk = ApiSdk.getApiSdk(appId,appKey);
            String jsonReturned = apiSdk.callInterFace(readsAndLikesApiUrl, map);
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

            logger.info(article.getTitle() + " " + article.getUrl() + " " + article.getPublish_time() + "\n" +
                    "Day 1: " + read_num[0] + " " + like_num[0] + "\n" +
                    "Day 2: " + read_num[1] + " " + like_num[1] + "\n" +
                    "Day 3: " + read_num[2] + " " + like_num[2] + "\n" +
                    "Day 4: " + read_num[3] + " " + like_num[3] + "\n" +
                    "Day 5: " + read_num[4] + " " + like_num[4] + "\n" +
                    "Day 6: " + read_num[5] + " " + like_num[5] + "\n" +
                    "Day 7: " + read_num[6] + " " + like_num[6] );

            for (int i = 0; i < 7; i++) {
                HashMap<Integer, Object> params = new HashMap<>();
                String add_time = null;
                try {
                    add_time = TimeUtil.convertDateToDateString(
                            TimeUtil.addDay(TimeUtil.convertStringToDate(date), i + 1));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (add_time.equals(TimeUtil.getTomorrowFormatDate())) {
                    break;
                }

                params.put(1, article.getId());
                params.put(2, read_num[i]);
                params.put(3, like_num[i]);
                params.put(4, add_time);
                try {
                    sourceDBServer.insert("wsa_article_stats", columns, params);
                    destDBServer.insert("wsa_article_stats", columns, params);
                } catch (Exception e) {
                    logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
                }
            }
        }

        sourceDBServer.close();
        destDBServer.close();
    }

    /**
     * 启动整个模块，对每一个帐号，对每一天执行操作
     */
    public void doRecover() {
        for (String account: accounts) {
            int temp = absentArticlesCounter;
            for (String date : dates) {
                Set<String> existArticlesTitle = getExistArticlesTitle(account, date);
                List<ArticleInfos> absentArticles = getAbsentArticles(existArticlesTitle, account, date);
                setArticleId(absentArticles);
                saveToRemoteDB(absentArticles);
                getReadsAndLikes(absentArticles, date);
            }
            if (absentArticlesCounter - temp > 0)
                logger.info(account + " lose " + (absentArticlesCounter - temp) +
                        " articles from " + dates[0] + " to " + dates[dates.length - 1]);
        }
        logger.info("Lose " + absentArticlesCounter+
                " articles from " + dates[0] + " to " + dates[dates.length - 1] + " totally.");
    }


    public static void main(String[] args) {
        AutoRecover autoRecover = new AutoRecover(2017, 6);
        autoRecover.doRecover();
    }
}
