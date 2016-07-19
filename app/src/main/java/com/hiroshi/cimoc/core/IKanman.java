package com.hiroshi.cimoc.core;

import com.hiroshi.cimoc.core.base.Manga;
import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.utils.Decryption;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Hiroshi on 2016/7/8.
 */
public class IKanman extends Manga {

    private String host = "http://m.ikanman.com";

    @Override
    protected String parseSearchUrl(String keyword, int page) {
        return host + "/s/" + keyword + ".html?page=" + page;
    }

    @Override
    protected List<Comic> parseSearch(String html) {
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("#detail > li > a");
        List<Comic> list = new LinkedList<>();
        for (Element item : items) {
            String path = item.attr("href");
            String image = item.select("div > img").first().attr("data-src");
            String status = item.select("div > i").first().text();
            String title = item.select("h3").first().text();
            String author = item.select("dl:eq(2) > dd").first().text();
            String update = item.select("dl:eq(5) > dd").first().text();
            list.add(new Comic(Kami.SOURCE_IKANMAN, path, image, title, author, null, status, update));
        }
        return list;
    }

    @Override
    protected String parseIntoUrl(String path) {
        return host + path;
    }

    @Override
    protected Comic parseInto(String html, List<Chapter> list) {
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("#chapterList > ul > li > a");
        for (Element item : items) {
            String c_title = item.select("b").first().text();
            String c_path = item.attr("href");
            list.add(new Chapter(c_title, c_path));
        }
        String title = doc.select(".main-bar > h1").first().text();
        Element detail = doc.getElementsByClass("book-detail").first();
        Element cont = detail.getElementsByClass("cont-list").first();
        String image = cont.select(".thumb > img").first().attr("src");
        String status = cont.select(".thumb > i").first().text();
        String update = cont.select("dl:eq(2) > dd").first().text();
        String author = cont.select("dl:eq(3) > dd > a").first().attr("title");
        Element node = detail.getElementById("bookIntro");
        String intro;
        if (node.select("p:eq(0)").isEmpty()) {
            intro = node.text();
        } else {
            intro = node.select("p:eq(0)").first().text();
        }
        return new Comic(0, null, image, title, author, intro, status, update);
    }

    @Override
    protected String parseBrowseUrl(String path) {
        return host + path;
    }

    @Override
    protected String[] parseBrowse(String html) {
        Pattern pattern = Pattern.compile("decryptDES\\(\"(.*?)\"\\)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            try {
                String str = matcher.group(1);
                String cipherStr = str.substring(8);
                String keyStr = str.substring(0, 8);
                String packed = Decryption.desDecrypt(keyStr, cipherStr);
                String result = Decryption.evalDecrypt(packed.substring(4));

                String jsonString = result.substring(11, result.length() - 9);
                JSONObject info = new JSONObject(jsonString);
                JSONArray array = info.getJSONArray("images");
                int count = info.getInt("count");
                String[] images = new String[count];
                for (int i = 0; i != count; ++i) {
                    images[i] = "http://i.hamreus.com:8080" + array.getString(i);
                }
                return images;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}