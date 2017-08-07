package net.sklcc.wechatsupporter.useless;

import net.sklcc.wechatsupporter.db.DBServer;
import net.sklcc.wechatsupporter.util.NetUtil;
import net.sklcc.wechatsupporter.util.TimeUtil;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class DataProvider {
    private static Logger logger = Logger.getLogger(DataProvider.class.getName());

    private Map<String, String> official_accounts;
    private List<String> biz_accounts;              //要获取key的公众号
    private List<String> redos_biz_accounts;        //要重新获取key的公众号
    private ArrayList<ArrayList<String>> tasks;     //要获取数据的文章
    private ArrayList<ArrayList<String>> redos;     //要重新获取数据的文章
    private Key key;

    /**
     * @Description 构造函数
     */
    public DataProvider() throws Exception {
        getAccounts();
        redos_biz_accounts = new ArrayList<String>();
        tasks = new ArrayList<>();
        redos = new ArrayList<>();
    }

    private void getAccounts() throws Exception {
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        official_accounts = new HashMap<String, String>();
        biz_accounts = new ArrayList<String>();

        String sql = "select official_account,biz from wsa_official_account"; //修改过了
        ResultSet rs = sourceDBServer.select(sql);
        while (rs.next()) {
            official_accounts.put(rs.getString(2), rs.getString(1));
            biz_accounts.add(rs.getString(2));
        }

        logger.info(official_accounts.size() + " official_accounts.");
        sourceDBServer.close();
    }


    /**
     * @Description 通过LogExtracter，更换key值
     */
    private void changeKey(String bizAccount) throws Exception {
        key = new LogExtracter().getKey(bizAccount);
        logger.info("key changed: " + key);
    }

    /**
     * @Description 访问微信接口获得数据
     * @param key key值
     * @param url 文章地址
     * @return 返回获得的数据
     * @throws IOException
     */
    private String getappmsgext(Key key, String url) throws Exception {

        Map<String, String> params = NetUtil.parseQuery(url);

        String api = "http://mp.weixin.qq.com/mp/getappmsgext";
        return Request.Post(api)
                .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 9_2_1 like Mac OS X) AppleWebKit/601.1.46 " +
                        "(KHTML, like Gecko)    Mobile/13D15 MicroMessenger/6.3.13 NetType/WIFI Language/zh_CN")
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7")
                .addHeader("Accept-Language", "zh-CN")
                .addHeader("Connection", "keep-alive")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .bodyForm(Form.form()
                        .add("__biz", params.get("__biz"))
                        .add("mid", params.get("mid"))
                        .add("sn", params.get("sn"))
                        .add("idx", params.get("idx"))
                        .add("f", "json")
                        .add("is_need_ad", "0")
                        .add("key", key.getKey())
                        .add("uin", key.getUin())
                        .add("is_only_read", "1").build())
                .execute().returnContent().asString();
    }

    /**
     * @Description 检索发布七天内的文章并保存到tasks
     * @throws SQLException
     */
    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    private void getTasks(String account) throws SQLException {
        tasks.clear();
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");

        ResultSet rs = sourceDBServer.select("select * from wsa_article where to_days(now()) - " +
                "to_days(publish_time) <= 7 and official_account = '" + account + "'");

        //noinspection Duplicates
        while (rs.next()) {
            ArrayList<String> row = new ArrayList<>();
            for (int i = 1; i <= 11; i += 1) {
                row.add(rs.getString(i));
            }
            tasks.add(row);
        }
        logger.info(account + "'s tasks size: " + tasks.size());
        sourceDBServer.close();
    }

    /**
     * @Description 解析微信接口返回的数据并将有效数据保存到数据库中
     * @throws SQLException
     */
    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    private boolean doTasks(String bizAccount) throws Exception {
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        logger.info(bizAccount + " tasks size: " + tasks.size());

        key = new LogExtracter().getKey(bizAccount);
        logger.info(bizAccount + " key: " + key);
        if (key == null) {
//            Thread.sleep(10000);
//            key = new LogExtracter().getKey(bizAccount);
            redos_biz_accounts.add(bizAccount);
            logger.info(bizAccount + " doesn't have key ");
            return false;
        }

        for (ArrayList<String> row : tasks) {
            try {
                Thread.sleep(4000);
                String id = row.get(0);
                String url = row.get(5);

                String response = getappmsgext(this.key, url);

                logger.info(response);

                JSONObject jsonObject = new JSONObject(response);
                JSONObject data = (JSONObject) jsonObject.get("appmsgstat");
                Integer read_num = (Integer) data.get("read_num");
                Integer like_num = (Integer) data.get("like_num");

                String columns = "article_id, read_count, like_count, add_time";
                HashMap<Integer, Object> params = new HashMap<>();
                params.put(1, id);
                params.put(2, read_num.toString());
                params.put(3, like_num.toString());
                params.put(4, TimeUtil.convertMillsToDateString(System.currentTimeMillis()));
                sourceDBServer.insert("wsa_article_stats", columns, params);       //修改过了

                logger.info(params.toString());
            } catch (Exception e) {
                logger.error(e.getClass() + " " + Arrays.toString(e.getStackTrace()));
                if (e instanceof JSONException) {
                    //changeKey(bizAccount);
                    redos_biz_accounts.add(bizAccount);
                }
                redos.add(row);
            }
        }
        sourceDBServer.close();
        return true;
    }

    /**
     * @Description 将获取数据失败的文章重新获取一次
     * @throws SQLException
     */
    private void redoTasks(String bizAccount) throws Exception {
        while (redos.size() > 0) {
            logger.info(bizAccount + "redo begins, redo nums: " + redos.size());
            tasks = new ArrayList<>(redos);
            redos.clear();

            doTasks(bizAccount);
        }
    }

    public void provideData() throws Exception {
        logger.info("provideData begins, accounts nums: " + biz_accounts.size());
        for (String bizAccount : biz_accounts) {
            getTasks(official_accounts.get(bizAccount));
            if (!doTasks(bizAccount)) {
                logger.info("Can't find: " + bizAccount);
                continue;
            }
            logger.info("Refind: " + bizAccount);
            redoTasks(bizAccount);
        }
        reProvideData();
    }

    private void reProvideData() throws Exception {
        while (redos_biz_accounts.size() > 0) {
            logger.info("redo provideData accounts begins, redo accounts nums: " + redos_biz_accounts.size());
            biz_accounts = new ArrayList<String>(redos_biz_accounts);
            redos_biz_accounts.clear();

            provideData();
        }
    }

    public static void main(String[] args) throws Exception {
//        String article_url = "http://mp.weixin.qq.com/s?__biz=MjM5MTM1NzQ3Mg%3D%3D&mid=2652379422&idx=2&sn=b1bb6f4a7d66cb8abb8c014e5f03eb2f&scene=4#wechat_redirect";
//        byte[] bytes = article_url.getBytes();
//        System.out.println(new DataProvider().getappmsgext(new LogExtracter().getKey(), new String(bytes, "utf-8")));
        DataProvider dp = new DataProvider();
    }

}
