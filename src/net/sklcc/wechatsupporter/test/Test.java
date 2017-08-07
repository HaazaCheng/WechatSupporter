package net.sklcc.wechatsupporter.test;

import cn.gsdata.index.ApiSdk;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hazza on 2016/12/6.
 */
public class Test {
    public static void main(String[] args) {
        String appId = "aJ2Vx03fS7j4LdxeSrb5";
        String appKey = "zgaWRumyOf586J7z7uiH9E2D4";
        String apiUrl = "http://open.gsdata.cn/api/wx/wxapi/wx_week_readnum";
        ApiSdk apiSdk = ApiSdk.getApiSdk(appId,appKey);
        Map<String, Object> map = new HashMap<>();

        map.put("start_time", "2017-04-04");
        map.put("end_time", "2017-04-04");
        map.put("url", "http://mp.weixin.qq.com/s?__biz=MjM5NDM0NDMwMw==&mid=2654437185&idx=1&sn=d371bd8437a7ea75bf191131ef68edac&chksm=bd4ae3418a3d6a57dda45215c46baf1e7be209f86442ccfae695b4cae9d2a383c5879924391b&scene=4#wechat_redirect");
        map.put("page", 0);
        map.put("rows", 10);

        String jsonReturned = apiSdk.callInterFace(apiUrl, map);
        System.out.println(jsonReturned);
        JSONObject jsonObject = new JSONObject(jsonReturned);
//
//        JSONArray jsonArray = (JSONArray) jsonObject.get("returnData");
//        System.out.println(jsonArray.toString());

    }
}
