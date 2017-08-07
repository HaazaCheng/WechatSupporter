package net.sklcc.wechatsupporter.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class NetUtil {
    private NetUtil() {}

    /**
     * @Description 从url中解析出有效参数
     * @param url
     * @return 返回保存有效参数的map
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException
     */
    public static Map<String, String> parseQuery(String url) throws Exception {
        URL urlObject = new URL(url);
        Map<String, String> params = new HashMap<>();
        String querystring = urlObject.getQuery();

        if (querystring == null) {
            return null;
        }
        for (String param : querystring.split("&")) {
            int split = param.indexOf("=");

            if (split == -1) {  //防止没有匹配到=
                continue;
            }
            int length = param.length();
            String key = URLDecoder.decode(param.substring(0, split), "utf-8");
            String value = "";

            if (split+1 < length) {
                value = URLDecoder.decode(param.substring(split+1, length), "utf-8");
            }

            if (!params.containsKey(key)) {
                params.put(key, value);
            }
        }
        return params;
    }

    public static void main(String[] args) throws Exception {
//        Map<String, String> map = NetUtil.parseQuery("http://www.test.com/?__biz=3214321==&mid=321423");
        Map<String, String> map = NetUtil.parseQuery("https://mp.weixin.qq.com/s?__biz=MjM5MjAxNDM4MA==&mid=2666141111&idx=1&sn=0559a628252bde18e91bff4f79dcbf23&chksm=bdb27cf48ac5f5e27835867fb6a7c81cfade695704be47834d166ae5ee835e5803a27bbcf20e&scene=4&key=79512945a1fcb0e2b3815c5b30d9c79a0025e9a3dfb45a6207ad329113c4d6502d180af1a03e539d4e1b874df78d2feb&ascene=14&uin=MjA5MjkyNjkyMQ%3D%3D&devicetype=Windows+8&version=62000050&pass_ticket=zS6DmrzvI0BHiu8mpEIYkC3ZcIT3YJV8USamdjOG1VbaJKRCsmzaQkEb6fz%2FbaO%2B");

        System.out.println(map.get("__biz") + map.get("mid"));
    }
}
