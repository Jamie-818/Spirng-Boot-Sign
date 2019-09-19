package com.show.sign.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;

/**
 * http 工具类 获取请求中的参数
 * 
 * @author show
 * @date 14:23 2019/5/29
 */
public class HttpUtils {
    /**
     * 将URL的参数和body参数合并
     * 
     * @author show
     * @date 14:24 2019/5/29
     * @param request
     */
    public static SortedMap<String, String> getAllParams(HttpServletRequest request) throws IOException {
        SortedMap<String, String> result = new TreeMap<>();
        // 获取URL上的参数
        getUrlParams(request, result);
        // 获取body参数
        getAllRequestParam(request, result);
        return result;
    }

    /**
     * 获取 Body 参数
     * 
     * @author show
     * @date 15:04 2019/5/30
     * @param request
     */
    public static void getAllRequestParam(final HttpServletRequest request, SortedMap<String, String> result)
        throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String str = "";
        StringBuilder wholeStr = new StringBuilder();
        // 一行一行的读取body体里面的内容；
        while ((str = reader.readLine()) != null) {
            wholeStr.append(str);
        }
        wholeStr.trimToSize();
        String s = wholeStr.toString();
        if (!StringUtils.isEmpty(s)) {
            // 转化成json对象
            Map<String, String> allRequestParam = JSONObject.parseObject(s, Map.class);
            // 将URL的参数和body参数进行合并
            for (Map.Entry entry : allRequestParam.entrySet()) {
                result.put((String)entry.getKey(), (String)entry.getValue());
            }
        }
    }

    /**
     * 获取url参数
     * 
     * @author show
     * @param request
     */
    public static void getUrlParams(HttpServletRequest request, SortedMap<String, String> result) {
        String param = "";
        try {
            String urlParam = request.getQueryString();
            if (urlParam != null) {
                param = URLDecoder.decode(urlParam, "utf-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] params = param.split("&");
        for (String s : params) {
            int index = s.indexOf("=");
            if (index != -1) {
                result.put(s.substring(0, index), s.substring(index + 1));
            }
        }
    }
}
