package net.sklcc.wechatsupporter;

import net.sklcc.wechatsupporter.util.TimeUtil;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by rahul on 7/11/16.
 */

/**
 *Description:该类为主类，按照抓取公众号文章，获得文章的阅读数与点赞数,同步本地服务器数据到阿里云数据库，多线程抓取微信文章Html源代码的业务逻辑完成微信后端的数据获取。
 */
public class WechatSupporter {
    static Logger logger = Logger.getLogger(WechatSupporter.class.getName());

    public static void main(String[] args) throws MalformedURLException {
        CrawlArticle crawler = new CrawlArticle();
        Counter counter = new Counter();
        syncMan syncman = new syncMan();

        if (TimeUtil.getWeekOfDate(new Date()).equals("星期一")) {
            logger.info("Start to update account infos.");
            AccountInfoUpdater accountInfoUpdater = new AccountInfoUpdater();
            try {
                accountInfoUpdater.doUpdate();
            } catch (Exception e) {
                logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
            }
        }

        logger.info("Start to crawl article...");
        crawler.crawl();

        /*logger.info("Start to get data..");
        try {
            DataProvider dataProvider = new DataProvider();
            dataProvider.provideData();
        } catch (Exception e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        }*/

        logger.info("Start to get data..");
        try {
            GsDataProvider gsDataProvider = new GsDataProvider();
            gsDataProvider.doTask();
        } catch (Exception e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        }

        logger.info("Start to count...");
        try {
            counter.doCount();
        } catch (Exception e) {
            logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
        }


        logger.info("Start to synchronize...");
        try {
            syncman.doSync();
        } catch (SQLException e) {
            logger.error(e.getClass() + "" + Arrays.toString(e.getStackTrace()));
        }

        logger.info("Start to get HTML...");
        ArrayList<Article> articles;
        articles = HTMLHelper.getArticles();

        int threadNums = 4;
        int block = articles.size() / threadNums;
        Thread[] threads = new Thread[threadNums];
        for (int i = 0; i < threadNums; i += 1) {
            int begin = block * i;
            int end = ((i == threadNums - 1) ? articles.size() : block * (i + 1));
            threads[i] = new Thread(new HTMLGetter(new ArrayList<>(articles.subList(begin, end))));
            threads[i].start();
        }
        for (int i = 0; i < threadNums; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        logger.info("Start to check deleted articles...");
        HTMLHelper.deleteCheck();
/*
        DateDataRecover dateDataRecover = new DateDataRecover();
        dateDataRecover.doTask();*/
    }
}
