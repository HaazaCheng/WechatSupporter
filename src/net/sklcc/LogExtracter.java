package net.sklcc;

import net.sklcc.util.NetUtil;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hazza on 7/11/16.
 */
@SuppressWarnings("FieldCanBeLocal")
public class LogExtracter {
    private static Logger logger = Logger.getLogger(DataProvider.class.getName());
    private static String logPath = "/home/sklcc/hazza/wechat/hijack-proxy/log/proxy.log";
    private static String logPattern = "\\[([^\\]]+)\\]\\s*\\[([^\\]]+)\\]\\s*\\[([^\\]]+)\\]\\s*-\\s+(.*)";    //正则表达式

    private ArrayList<String> querys;   //保存要匹配的代理服务器日志句段
    private Pattern pattern;

    private List<Key> keyList;    //保存匹配到的key值

    /**
     * @Description 构造函数
     */
    LogExtracter() {
        querys = new ArrayList<>();
        keyList = new ArrayList<>();
        pattern = Pattern.compile(logPattern);
    }

    /**
     * @Description 读取代理服务器日志中的每行语句
     */
    private void getQueryString() {
        querys.clear();

        try {
            FileReader reader = new FileReader(logPath);
            BufferedReader bfReader = new BufferedReader(reader);

            String tempLine;
            while ((tempLine = bfReader.readLine()) != null) {
                Matcher m = pattern.matcher(tempLine);
                if (m.find()) {
                    String json = m.group(4);
                    if (json.charAt(0) != '{') continue;
                    JSONObject tempObject = new JSONObject(json);

                    if (tempObject.getJSONObject("request").getJSONObject("headers").has("referer")) {
                        String refererUrl = tempObject.getJSONObject("request").getJSONObject("headers").getString("referer");
                        querys.add(refererUrl);
                    }
                }
            }

            bfReader.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description  获得匹配到的有效参数
     * @param query 需要正则匹配的语句
     * @throws UnsupportedEncodingException
     */
    private void getQueryParams(String query) throws Exception {
        if (query == null) return;

        if (NetUtil.parseQuery(query) != null) {
            Map<String, String> params = new HashMap<String, String>(NetUtil.parseQuery(query));
            if (params.containsKey("__biz") && params.containsKey("key") && params.containsKey("uin")) {
                keyList.add(new Key(params.get("__biz"), params.get("key"), params.get("uin")));
//                logger.info("accepted key : " + (keyList.get(keyList.size() - 1).toString()));
            }
        }
    }


    /**
     * @Description 逐一将每行语句匹配正则表达式
     */
    private void getKeys() throws Exception {
        for (String s : querys) {
            try {
                //logger.info("query : " + s);
                getQueryParams(s);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        logger.info("总共有" + keyList.size() + "个key");
    }

    /**
     * @Description 获得key值
     * @return 返回最新的key值
     */
    Key getKey(String bizAccount) throws Exception {
        this.getQueryString();
        this.getKeys();

        logger.info("keyList.seize():" + keyList.size());

        for (int i = keyList.size() - 1; i >= 0; --i) {
            //logger.info("keyList.get(i).getBiz():" + keyList.get(i).getBiz() +" bizAccount:" + bizAccount);
            if (keyList.get(i).getBiz().equals(bizAccount)) {
                return keyList.get(i);
            }
        }

        return null;
    }

    public static void main(String[] args) throws Exception {
        LogExtracter le = new LogExtracter();
        //System.out.println(le.getKey());
    }
}

/**
 * @Description key的定义，保存key和uin
 */
class Key {
    private String __biz;
    private String key;
    private String uin;


    Key(String biz, String key, String uin) {
        this.__biz = biz;
        this.key = key;
        this.uin = uin;
    }

    @SuppressWarnings("unused")
    Key(List<String> keyList, List<String> uinList) {
        this.key = keyList.get(0);
        this.uin = uinList.get(0);
    }


    String getBiz() {
        return this.__biz;
    }


    String getKey() {
        return this.key;
    }


    String getUin() {
        return this.uin;
    }

    public String toString() {
        return "__biz=" + this.__biz + "  key=" + this.key + " uin=" + this.uin;
    }

    public static void main(String[] args) throws Exception{
        //String query = "https://mp.weixin.qq.com/s?__biz=MjM5MjAxNDM4MA==&mid=2666141111&idx=1&sn=0559a628252bde18e91bff4f79dcbf23&chksm=bdb27cf48ac5f5e27835867fb6a7c81cfade695704be47834d166ae5ee835e5803a27bbcf20e&scene=4&key=79512945a1fcb0e2b3815c5b30d9c79a0025e9a3dfb45a6207ad329113c4d6502d180af1a03e539d4e1b874df78d2feb&ascene=14&uin=MjA5MjkyNjkyMQ%3D%3D&devicetype=Windows+8&version=62000050&pass_ticket=zS6DmrzvI0BHiu8mpEIYkC3ZcIT3YJV8USamdjOG1VbaJKRCsmzaQkEb6fz%2FbaO%2B";
        String query = "https://mp.weixin.qq.com/s?__biz=MzAxNzI0MDc3Mg==&mid=400701426&idx=1&sn=2d1bb55744d7d62544058668610f3133&scene=4&key=c3acc508db7203762f92ebbca1a3c4858831d4df5a1a109850088eb144343b8ebaeb33e166f7a7c5c12b22b92b56694f&ascene=7&uin=MjU2NTcyMjcyNw%3D%3D&devicetype=Windows+XP&version=6202002e&pass_ticket=tNygZcLaJtiucUmc%2F8SQIQvs0zx5%2FG20USvwEmWA35KjD4Ff2XCfuM7MSDqx3eG%2B\n";
        Map<String, String> params = NetUtil.parseQuery(query);

        if (params.containsKey("__biz") && params.containsKey("key") && params.containsKey("uin")) {
            Key key = new Key(params.get("__biz"), params.get("key"), params.get("uin"));
            System.out.println(key.toString());
        }
    }
}