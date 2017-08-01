package net.sklcc;

import net.sklcc.db.DBServer;
import net.sklcc.util.TimeUtil;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @Description 多线程类获取HTML代码源码
 */
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class HTMLGetter implements Runnable {

    private ArrayList<Article> errorTasks;  //保存获取HTML源代码错误的文章

    public HTMLGetter(ArrayList<Article> articles) {
        this.errorTasks = articles;
    }

    /**
     * @Description 开始线程
     */
    public void run() {

        while (errorTasks.size() > 0) {
            HTMLHelper.logger.info("HTMLGetter thread, task size: " + errorTasks.size());
            ArrayList<Article> temp = new ArrayList<>(errorTasks);
            errorTasks.clear();
            for (Article article : temp) {
                try {
                    String oldContent = HTMLHelper.exists(article.id);
                    String newContent = HTMLHelper.getContent(article.url);
                    if (oldContent == null) {
                        HTMLHelper.insertContent(article.id, newContent);
                    } else {
                        HTMLHelper.setDeleteFlag(article, newContent, oldContent);
                    }
                } catch (Exception e) {
                    errorTasks.add(article);
                    HTMLHelper.logger.error("error article: " + article);
                    HTMLHelper.logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception{
       /* String id = "145917";
        String url = "http://mp.weixin.qq.com/s?__biz=MjM5MTM1NzQ3Mg==&mid=2652382266&idx=3&sn=897e6020c0f43850c4256aa0b8eb08eb&scene=4#wechat_redirect";
        String oldContent = HTMLHelper.exists(id);
        String newContent = HTMLHelper.getContent(url);
        HTMLHelper.setDeleteFlag(id, newContent, oldContent);*/

        HTMLHelper.deleteCheckAll();
    }
}

/**
 * @Description HTMLGetter的工具类
 */
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
class HTMLHelper {
    static Logger logger = Logger.getLogger(HTMLHelper.class.getName());
    static List<Article> delArticles;

    /**
     * @Description 从数据库获得最近七天的文章
     * @return 获得的文章
     */
    static ArrayList<Article> getArticles() {
        ArrayList<Article> articles = new ArrayList<>();
        delArticles = Collections.synchronizedList(new ArrayList<>());

        String sql = "select id, url from wsa_article where to_days(now()) - to_days(publish_time) <= 7";
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        try {
            ResultSet rs = sourceDBServer.select(sql);
            while (rs.next()) {
                articles.add(new Article(rs.getString(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        } finally {
            sourceDBServer.close();
        }

        return articles;
    }

    /**
     * @Description 从文章地址获得HTML源码
     * @param url 文章地址
     * @return HTML源代码
     * @throws IOException
     */
    public static String getContent(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        doc.select("script").remove();
        return new String(Base64.getEncoder().encode(doc.toString().getBytes()));
    }

    /**
     * @Description 判断html是否存在
     * @param id 文章id
     * @return 如果存在，返回html代码，如果不存在，返回null
     * @throws SQLException
     */
    static String exists(String id) throws SQLException {
        String res = null;

        DBServer sourceDBServer = new DBServer("proxool.sourceDb");

        String sql = "select content from wsa_article_content where article_id = " + id;
        ResultSet rs = sourceDBServer.select(sql);
        if (rs.next()) {
            res = rs.getString(1);
        }

        sourceDBServer.close();
        return res;
    }


    /**
     * @Description 如果html代码不存在，将html源码保存到数据库
     * @param id 文章id
     * @param content 文章内容
     * @throws SQLException
     */
    static void insertContent(String id, String content) throws Exception {

        DBServer sourceDBServer = new DBServer("proxool.sourceDb");

        String columns = "article_id, content";
        HashMap<Integer, Object> params = new HashMap<>();
        params.put(1, id);
        params.put(2, content);
        sourceDBServer.insert("wsa_article_content", columns, params);

        logger.info("Content inserted, article_id: " + id);

        sourceDBServer.close();
    }

    /**
     * @Description 通过html长度，判断文章内容是否改变，若改变在数据库中设置flag
     * @param article 文章
     * @param newContent  新的HTML代码
     * @param oldContent  数据库中保存的旧的html代码
     * @throws SQLException
     */
    static void setDeleteFlag(Article article, String newContent, String oldContent) throws SQLException, ClassNotFoundException {
        Boolean modified = false;

        if (newContent.length() == oldContent.length() && newContent.equals(oldContent)) {
            return;
        }

        if (newContent.length() > 20000) {
            modified = true;
        }

        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        DBServer destDBServer = new DBServer("proxool.destDb");
        String sql;
        String columns,condition;

        if (modified) {
            columns = "deleted, delete_time";
            condition = "where id = '" + article.id + "'";
            HashMap<Integer, Object> params = new HashMap<>();
            params.put(1, 2);
            params.put(2, TimeUtil.convertMillsToDateString(System.currentTimeMillis()));

            sourceDBServer.update("wsa_article", columns, condition, params);
            destDBServer.update("wsa_article", columns, condition, params);

        } else {
            String deleted;
            sql = "select deleted from wsa_article where id = " + article.id;
            ResultSet rs = sourceDBServer.select(sql);
            rs.next();
            deleted = rs.getString(1);
            if (deleted.equals("1")) {
                logger.warn("Content had been deleted already!");
                sourceDBServer.close();
                destDBServer.close();
                return;
            }

            columns = "deleted, delete_time";
            condition = "where id = '" + article.id + "'";
            HashMap<Integer, Object> params = new HashMap<>();
            params.put(1, 1);
            params.put(2, TimeUtil.convertMillsToDateString(System.currentTimeMillis()));

            sourceDBServer.update("wsa_article", columns, condition, params);
            destDBServer.update("wsa_article", columns, condition, params);

            delArticles.add(article);

            logger.info("Content deleted, article_id: " + article.id);
        }
        sourceDBServer.close();
        destDBServer.close();
    }

    /**
     * @Description 将HTML代码保存到本地文件中
     * @param id
     * @throws SQLException
     * @throws FileNotFoundException
     */
    @SuppressWarnings("unused")
    static void saveToFile(String id) throws SQLException, FileNotFoundException {
        byte[] html = Base64.getDecoder().decode(exists(id).getBytes());

        PrintWriter pw = new PrintWriter(id+".html");
        pw.write(new String(html));
        pw.flush();
        pw.close();
    }

    /**
     * 检索数据库中所有被删除的文章，通过关键字检查是否真的被删除，防止因为字数过少而被删除
     */
    @SuppressWarnings("unused")
    static void deleteCheckAll() {
        ArrayList<Article> deletedArticles = new ArrayList<>();

        String sql = "select id, url from wsa_article where deleted = 1";
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        DBServer destDBServer = new DBServer("proxool.destDb");
        try {
            ResultSet rs = sourceDBServer.select(sql);
            while (rs.next()) {
                deletedArticles.add(new Article(rs.getString(1), rs.getString(2)));
            }

            for (Article article: deletedArticles) {
                Document doc = Jsoup.connect(article.url).get();
                doc.select("script").remove();
                if (doc.toString().indexOf("违规无法查看") == -1 && doc.toString().indexOf("发布者删除") == -1) {     //该文章字数少于20000，但并未被删除，属于误删
                    String columns = "deleted, delete_time";
                    String condition = "where id = '" + article.id + "'";
                    HashMap<Integer, Object> params = new HashMap<>();
                    params.put(1, 0);
                    params.put(2, null);
                    sourceDBServer.update("wsa_article", columns, condition, params);
                    destDBServer.update("wsa_article", columns, condition, params);
                    logger.info("Articile: " + article.url + " was deleted wrong!");
                }
            }
        } catch (Exception e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        } finally {
            sourceDBServer.close();
            destDBServer.close();
        }

    }

    static void deleteCheck() {
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        DBServer destDBServer = new DBServer("proxool.destDb");
        try {
            logger.info("There are " + delArticles.size() + " to be checked.");
            for (Article article: delArticles) {
                Document doc = Jsoup.connect(article.url).get();
                doc.select("script").remove();
                if (doc.toString().indexOf("违规无法查看") == -1 && doc.toString().indexOf("发布者删除") == -1) {     //该文章字数少于20000，但并未被删除，属于误删
                    String columns = "deleted, delete_time";
                    String condition = "where id = '" + article.id + "'";
                    HashMap<Integer, Object> params = new HashMap<>();
                    params.put(1, 0);
                    params.put(2, null);
                    sourceDBServer.update("wsa_article", columns, condition, params);
                    destDBServer.update("wsa_article", columns, condition, params);
                    logger.info("Articile: " + article.url + " was deleted wrong!");
                }
            }
        } catch (Exception e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        } finally {
            sourceDBServer.close();
            destDBServer.close();
        }

    }
}

/**
 * @Description 文章的定义结构体
 */
@SuppressWarnings("ALL")
class Article {
    String id;  //文章在数据中的id
    String url; //文章地址
    public Article(String i, String str) {
        id = i;
        url = str;
    }

    public String toString() {
        return "id="+id+" url="+url;
    }
}