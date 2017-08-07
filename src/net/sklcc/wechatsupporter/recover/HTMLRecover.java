package net.sklcc.wechatsupporter.recover;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Base64;

/**
 * Created by Hazza on 2017/3/20.
 */
public class HTMLRecover {

    private static String getContent(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        doc.select("script").remove();
        boolean flag = false;
        if (doc.toString().indexOf("删除") != -1) {
            flag = true;
        }
        System.out.println(flag);
        return new String(Base64.getEncoder().encode(doc.toString().getBytes()));
    }

    public static void main(String[] args) {

        try {
            System.out.println(HTMLRecover.getContent("http://mp.weixin.qq.com/s?__biz=MzI3MTE3MDY2MQ==&mid=2651267820&idx=2&sn=a5e576be872310a25ebc15d18ac55c4b&chksm=f1366fe6c641e6f0a03cdcdde64e9e10388f200dfc1a1417b3688e5a94c2c57d7267be00d6a5&scene=4#wechat_redirect").length());
            System.out.println(HTMLRecover.getContent("http://mp.weixin.qq.com/s?__biz=MzA5MTE3ODY5MQ==&mid=2656911330&idx=8&sn=3d04733803551e3401cb000c442edf6d&chksm=8ba9cc4ebcde4558daf280a6d56cd228656dda0de5ee77dedfd80c8f2fb42e3f805697a27dbd&scene=4#wechat_redirect").length());
            System.out.println(HTMLRecover.getContent("http://mp.weixin.qq.com/s?__biz=MjM5OTM4MzkzMw==&mid=2657249925&idx=2&sn=3c60e1483e345b57a7f017cfaa85a377&chksm=bcaa8c978bdd0581aa99afd02c0132615ad1b2aa2fc7caaafe58f57b37aa4cd28a9899888a9e&scene=4#wechat_redirect").length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
