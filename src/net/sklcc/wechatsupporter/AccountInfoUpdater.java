package net.sklcc.wechatsupporter;

import cn.gsdata.index.ApiSdk;
import net.sklcc.wechatsupporter.db.DBServer;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hazza on 2017/3/4.
 */
public class AccountInfoUpdater {
    private static Logger logger = Logger.getLogger(CrawlArticle.class.getName());

    private final static String appId = "";
    private final static String appKey = "";
    private final static String apiUrl = "";

    List<String[]> accounts;

    void getAccountsInfos() throws SQLException {
        accounts = new ArrayList<>();
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");

        String sql = "select official_account, nickname, qr_code, description, authentication, authentication_info from wsa_official_account";
        ResultSet rs = sourceDBServer.select(sql);
        while (rs.next()) {
            String[] infos = new String[6];
            for (int i =0; i < 6; ++i) {
                infos[i] = rs.getString(i + 1);
            }
            accounts.add(infos);
        }

        logger.info(accounts.size() + " official_accounts are needed to be checked.");
        sourceDBServer.close();
    }

    void doUpdate() throws Exception {
        getAccountsInfos();
        ApiSdk apiSdk = ApiSdk.getApiSdk(appId,appKey);
        DBServer sourceDBServer = new DBServer("proxool.sourceDb");
        DBServer destDBServer = new DBServer("proxool.destDb");

        for (String[] account: accounts) {
            logger.info("Start tp check " + account[0]);
            Map<String, Object> map = new HashMap<>();
            map.put("wx_name", account[0]);

            String jsonReturned = apiSdk.callInterFace(apiUrl, map);
            JSONObject jsonObject = new JSONObject(jsonReturned);
            try {
                JSONObject accountInfo = jsonObject.getJSONObject("returnData");

                StringBuffer columns = new StringBuffer();
                HashMap<Integer, Object> params = new HashMap<Integer, Object>();
                String condition = "where official_account = '" + account[0] + "'";
                boolean flag = false;
                int count = 0;

                String nickname = (String) accountInfo.get("wx_nickname");      //数据库中的nickname字段
                if (!nickname.equals(account[1])) {
                    flag = true;
                    columns.append("nickname,");
                    params.put(++count, nickname);

                    logger.info(account[0] + "'s nickname has changed, from " + account[1] + " to " + nickname);
                }

                String qr_code = "无";                                           //数据库中的qr_code字段
                if (accountInfo.has("wx_qrcode")) {
                    qr_code = (String) accountInfo.get("wx_qrcode");
                }
                if (!qr_code.equals(account[2])){
                    flag = true;
                    columns.append("qr_code,");
                    params.put(++count, qr_code);
                    logger.info(account[0] + "'s qr_code has changed");
                }

                String description = "无";                                       //数据库中的description字段
                if (accountInfo.has("wx_note")) {
                    description = (String) accountInfo.get("wx_note");
                }
                if (!description.equals(account[3])){
                    flag = true;
                    columns.append("description,");
                    params.put(++count, description);
                    logger.info(account[0] + "'s description has changed");
                }

                String authentication = "未认证";                                 //数据库中的authentication字段
                if (accountInfo.has("wx_vip")) {
                    authentication = (String) accountInfo.get("wx_vip");
                }
                if (!authentication.equals(account[4])){
                    flag = true;
                    columns.append("authentication,");
                    params.put(++count, authentication);
                    logger.info(account[0] + "'s authentication has changed");
                }

                String authentication_info = "无";                               //数据库中的authentication_info字段
                if (accountInfo.has("wx_vip_note")) {
                    authentication_info = (String) accountInfo.get("wx_vip_note");
                }
                if (!authentication_info.equals(account[4])){
                    flag = true;
                    columns.append("authentication_info,");
                    params.put(++count, authentication_info);
                    logger.info(account[0] + "'s authentication_info has changed");
                }

                if (flag) {
                    String column = columns.toString().substring(0, columns.length()-1);

                    sourceDBServer.update("wsa_official_account", column, condition, params);
                    destDBServer.update("wsa_official_account", column, condition, params);
                }
            } catch (JSONException e) {
                logger.info(account[0] + "don't have infomation.");
            }
        }
        sourceDBServer.close();
        destDBServer.close();
    }


    public static void main(String[] args) {
        AccountInfoUpdater accountInfoUpdater = new AccountInfoUpdater();
        try {
            accountInfoUpdater.doUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
