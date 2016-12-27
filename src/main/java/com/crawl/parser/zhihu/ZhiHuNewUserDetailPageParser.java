package com.crawl.parser.zhihu;

import com.crawl.entity.Page;
import com.crawl.entity.User;
import com.crawl.parser.DetailPageParser;
import com.crawl.util.Constants;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wy on 11/28/2016.
 * https://www.zhihu.com/people/wo-yan-chen-mo/following
 * 新版following页面解析出用户详细信息
 */
public abstract class ZhiHuNewUserDetailPageParser extends DetailPageParser{
    @Override
    public User parse(Page page) {
        Document doc = Jsoup.parse(page.getHtml());
        User user = new User();
        String userId = getUserId(page.getUrl());
        user.setUrl("https://www.zhihu.com/people/" + userId);//用户主页
        String parseStrategy = getParseStrategy();
        String type = null;
        if (parseStrategy.equals(Constants.LOGIN_PARSE_STRATEGY)){
            //登录模式
            type = userId;
        }
        else if (parseStrategy.equals(Constants.TOURIST_PARSE_STRATEGY)){
            //游客模式
            type = "null";
        }
        getUserByJson(user, type, doc.select("[data-state]").first().attr("data-state"));
        return user;
    }
    private void getUserByJson(User user, String type, String dataStateJson){
        type = "['" + type + "']";//转义
        String commonJsonPath = "$.entities.users." + type;
        setUserInfoByJsonPth(user, "username", dataStateJson, commonJsonPath + ".name");//username
        setUserInfoByJsonPth(user, "hashId", dataStateJson, commonJsonPath + ".id");//hashId
        setUserInfoByJsonPth(user, "followees", dataStateJson, commonJsonPath + ".followingCount");//关注人数
        setUserInfoByJsonPth(user, "location", dataStateJson, commonJsonPath + ".locations[0].name");//位置
        setUserInfoByJsonPth(user, "business", dataStateJson, commonJsonPath + ".business.name");//行业
        setUserInfoByJsonPth(user, "employment", dataStateJson, commonJsonPath + ".employments[0].company.name");//公司
        setUserInfoByJsonPth(user, "position", dataStateJson, commonJsonPath + ".employments[0].job.name");//职位
        setUserInfoByJsonPth(user, "education", dataStateJson, commonJsonPath + ".educations[0].school.name");//学校
        setUserInfoByJsonPth(user, "answers", dataStateJson, commonJsonPath + ".answerCount");//回答数
        setUserInfoByJsonPth(user, "asks", dataStateJson, commonJsonPath + ".questionCount");//提问数
        setUserInfoByJsonPth(user, "posts", dataStateJson, commonJsonPath + ".articlesCount");//文章数
        setUserInfoByJsonPth(user, "followers", dataStateJson, commonJsonPath + ".followerCount");//粉丝数
        setUserInfoByJsonPth(user, "agrees", dataStateJson, commonJsonPath + ".voteupCount");//赞同数
        setUserInfoByJsonPth(user, "thanks", dataStateJson, commonJsonPath + ".thankedCount");//感谢数
        Integer gender = JsonPath.parse(dataStateJson).read(commonJsonPath + ".gender");
        if (gender != null && gender == 1){
            user.setSex("male");
        }
        else if(gender != null && gender == 0){
            user.setSex("female");
        }
    }

    /**
     * jsonPath获取值，并通过反射直接注入到user中
     * @param user
     * @param fieldName
     * @param json
     * @param jsonPath
     */
    private void setUserInfoByJsonPth(User user, String fieldName, String json, String jsonPath){
        try {
            Object o = JsonPath.parse(json).read(jsonPath);
            Field field = user.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(user, o);
        } catch (PathNotFoundException e1) {
            //no results
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 根据url解析出用户id
     * @param url
     * @return
     */
    private String getUserId(String url){
        Pattern pattern = Pattern.compile("https://www.zhihu.com/[a-z]+/(.*)/following");
        Matcher matcher = pattern.matcher(url);
        String userId = null;
        if(matcher.find()){
            userId = matcher.group(1);
            return userId;
        }
        throw new RuntimeException("not parse userId");
    }
    public abstract String getParseStrategy();
}