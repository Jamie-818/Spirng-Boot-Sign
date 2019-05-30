# 简介
- 现在越来越多人关注接口安全，传统的接口在传输的过程中，容易被抓包然后更改里面的参数值达到某些目的。
- 传统的做法是用安全框架或者在代码里面做验证，但是有些系统是不需要登录的，随时可以调。
- 这时候我们可以通过对参数进行签名验证，如果参数与签名值不匹配，则请求不通过，直接返回错误信息。
# 项目代码地址：
   - [https://github.com/MrXuan3168/sign_server](https://github.com/MrXuan3168/sign_server)
# 测试
   1. 启动项目
   2. GET请求可以用浏览器直接访问  http://localhost:8080/signTest?sign=A0161DC47118062053567CDD10FBACC6&username=admin&password=admin
      - A0161DC47118062053567CDD10FBACC6 是 username=admin&password=admin MD5加密后的结果。可以打开 https://md5jiami.51240.com/ 然后输入 {"password":"admin","username":"admin"} 进行加密验证，json字符串里面，必须保证字段是按照 ascll码 
      进行排序的,username的ascll码 比 password的ascll码 大，所以要放在后面。
   3. 打开 postman 进行POST请求测试，请求Url为 http://localhost:8080/signTest?sign=A0161DC47118062053567CDD10FBACC6 参数为
      ```json
        {
            "username":"admin",
            "password":"admin"
        }
      ```
![成功](https://upload-images.jianshu.io/upload_images/13183199-e0af7e0a7ee986d9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
![失败](https://upload-images.jianshu.io/upload_images/13183199-a26affcf8a3eee62.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
# 调用过程
![](https://upload-images.jianshu.io/upload_images/13183199-baaaaa49567dc4cc.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# 涉及第三方技术
- 前端：js-md5(vue md5-npm包)、axios(vue ajax请求npm包)
   - 安装命令
   ```
    npm install --save js-md5
    npm install axios
   ```
- 后端: fastjson、lombok
   ```XML 
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.47</version>
            <scope>compile</scope>
        </dependency>
   ```
# 签名逻辑
- 前端（客户端）：
   1.不管GET Ulr 还是 POST Body 的参数，都转换成 json 对象，用 ascll码排序 对参数排序。
   2.排序后对参数进行MD5加密，存入 sign 值。
   3.把 sign 值 放在 请求URL 后面或者 Head头 里面(该项目直接放在URL后面)。
- 后端（服务端）：
   1.把参数接收，转成 json对象 ，用 ascll码 排序
   2.排序后对参数进行MD5加密，存入 paramsSign 值。
   3.和 请求URL 中的 sign值 做对比，相同则请求通过。
# 前端代码
- 加密工具类
```javaScript
import md5 from 'js-md5'

export default class signMd5Utils {
    /**
     * json参数升序
     * @param jsonObj 发送参数
     */

    static sortAsc(jsonObj) {
        let arr = new Array();
        let num = 0;
        for (let i in jsonObj) {
            arr[num] = i;
            num++;
        }
        let sortArr = arr.sort();
        let sortObj = {};
        for (let i in sortArr) {
            sortObj[sortArr[i]] = jsonObj[sortArr[i]];
        }
        return sortObj;
    }


    /**
     * @param url 请求的url,应该包含请求参数(url的?后面的参数)
     * @param requestParams 请求参数(POST的JSON参数)
     * @returns {string} 获取签名
     */
    static getSign(url, requestParams) {
        let urlParams = this.parseQueryString(url);
        let jsonObj = this.mergeObject(urlParams, requestParams);
        let requestBody = this.sortAsc(jsonObj);
        return md5(JSON.stringify(requestBody)).toUpperCase();
    }

    /**
     * @param url 请求的url
     * @returns {{}} 将url中请求参数组装成json对象(url的?后面的参数)
     */
    static parseQueryString(url) {
        let urlReg = /^[^\?]+\?([\w\W]+)$/,
            paramReg = /([^&=]+)=([\w\W]*?)(&|$|#)/g,
            urlArray = urlReg.exec(url),
            result = {};
        if (urlArray && urlArray[1]) {
            let paramString = urlArray[1], paramResult;
            while ((paramResult = paramReg.exec(paramString)) != null) {
                result[paramResult[1]] = paramResult[2];
            }
        }
        return result;
    }

    /**
     * @returns {*} 将两个对象合并成一个
     */
    static mergeObject(objectOne, objectTwo) {
        if (Object.keys(objectTwo).length > 0) {
            for (let key in objectTwo) {
                if (objectTwo.hasOwnProperty(key) === true) {
                    objectOne[key] = objectTwo[key];
                }
            }
        }
        return objectOne;
    }

    static urlEncode(param, key, encode) {
        if (param == null) return '';
        let paramStr = '';
        let t = typeof (param);
        if (t == 'string' || t == 'number' || t == 'boolean') {
            paramStr += '&' + key + '=' + ((encode == null || encode) ? encodeURIComponent(param) : param);
        } else {
            for (let i in param) {
                let k = key == null ? i : key + (param instanceof Array ? '[' + i + ']' : '.' + i);
                paramStr += this.urlEncode(param[i], k, encode);
            }
        }
        return paramStr;
    };
}
```
- 发送请求类
```javaScript
import axios from 'axios';
import signMd5Utils from "../utils/signMd5Utils"
// var config = require('../../config')
//config = process.env.NODE_ENV === 'development' ? config.dev : config.build
//let apiUrl = config.apiUrl;
//var qs = require('qs');
const instance = axios.create({
    baseURL: 'http://localhost:8080/',
    // timeout: 1000 * 30,
    // 允许跨域带token
    xhrFields: {
        withCredentials: false
    },
    crossDomain: true,
    emulateJSON: true
});
export default instance
export function signTestPost(query) {

    let url = 'signTest';
    let sign = signMd5Utils.getSign(url, query);
    let requestUrl = url + "?sign=" + sign;  //将签名添加在请求参数后面去请求接口
    return instance({
        url: requestUrl,
        method: 'post',
        data: query
    })
}
export function signTestGet(query) {

    let url = 'signTest';
    let urlParams = signMd5Utils.urlEncode(query);
    let sign = signMd5Utils.getSign(url, query);
    let requestUrl = url + "?sign=" + sign + urlParams;  //将签名添加在请求参数后面去请求接口
    return instance({
        url: requestUrl,
        method: 'get',
    })
}
```
- 调用请求
```javaScript
let user = {
    "username": "admin",
    "password": "admin",
};
signTestPost(user).then(r => {
    console.log(r)
});

signTestGet(user).then(r => {
    console.log(r)
})
```
# 后端代码
 - 过滤器(到达 Controller 前执行)
```java
import com.alibaba.fastjson.JSONObject;
import com.show.sign.utils.HttpUtils;
import com.show.sign.utils.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedMap;

/**
 * 签名过滤器
 * @author show
 * @date 10:03 2019/5/30
 * @Component 注册 Filter 组件
 */
@Slf4j
@Component 
public class SignAuthFilter implements Filter {
    static final String FAVICON = "/favicon.ico";

    @Override
    public void init(FilterConfig filterConfig) {

        log.info("初始化 SignAuthFilter");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        // 防止流读取一次后就没有了, 所以需要将流继续写出去
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletRequest requestWrapper = new BodyReaderHttpServletRequestWrapper(request);
        //获取图标不需要验证签名
        if (FAVICON.equals(requestWrapper.getRequestURI())) {
            chain.doFilter(request, response);
        } else {
            //获取全部参数(包括URL和body上的)
            SortedMap<String, String> allParams = HttpUtils.getAllParams(requestWrapper);
            //对参数进行签名验证
            boolean isSigned = SignUtil.verifySign(allParams);
            if (isSigned) {
                log.info("签名通过");
                chain.doFilter(requestWrapper, response);
            } else {
                log.info("参数校验出错");
                //校验失败返回前端
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json; charset=utf-8");
                PrintWriter out = response.getWriter();
               JSONObject resParam = new JSONObject();
                resParam.put("msg", "参数校验出错");
                resParam.put("success", "false");
                out.append(resParam.toJSONString());
            }
        }
    }

    @Override
    public void destroy() {

        log.info("销毁 SignAuthFilter");
    }
}
```
- BodyReaderHttpServletRequestWrapper 类 主要作用是复制 HttpServletRequest 的输入流，不然你拿出 body 参数后验签后，到 Controller 时，接收参数会为 null
```java
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;

/**
 * 保存过滤器里面的流
 * @author show
 * @date 10:03 2019/5/30
 */
public class BodyReaderHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public BodyReaderHttpServletRequestWrapper(HttpServletRequest request) {

        super(request);
        String sessionStream = getBodyString(request);
        body = sessionStream.getBytes(Charset.forName("UTF-8"));
    }

    /**
     * 获取请求Body
     *
     * @param request
     * @return
     */
    public String getBodyString(final ServletRequest request) {

        StringBuilder sb = new StringBuilder();
        try (
            InputStream inputStream = cloneInputStream(request.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * Description: 复制输入流</br>
     *
     * @param inputStream
     * @return</br>
     */
    public InputStream cloneInputStream(ServletInputStream inputStream) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buffer)) > -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            byteArrayOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    @Override
    public BufferedReader getReader() {

        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() {

        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {

            @Override
            public int read() {

                return bais.read();
            }

            @Override
            public boolean isFinished() {

                return false;
            }

            @Override
            public boolean isReady() {

                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}
```
- 签名工具类
```java
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.SortedMap;

/**
 * 签名工具类
 * @author show
 * @date 10:01 2019/5/30
 */
@Slf4j
public class SignUtil {

    /**
     * @param params 所有的请求参数都会在这里进行排序加密
     * @return 验证签名结果
     */
    public static boolean verifySign(SortedMap<String, String> params) {

        String urlSign = params.get("sign");
        log.info("Url Sign : {}", urlSign);
        if (params == null || StringUtils.isEmpty(urlSign)) {
            return false;
        }
        //把参数加密
        String paramsSign = getParamsSign(params);
        log.info("Param Sign : {}", paramsSign);
        return !StringUtils.isEmpty(paramsSign) && urlSign.equals(paramsSign);
    }

    /**
     * @param params 所有的请求参数都会在这里进行排序加密
     * @return 得到签名
     */
    public static String getParamsSign(SortedMap<String, String> params) {
        //要先去掉 Url 里的 Sign
        params.remove("sign");
        String paramsJsonStr = JSONObject.toJSONString(params);
        return DigestUtils.md5DigestAsHex(paramsJsonStr.getBytes()).toUpperCase();
    }
}
```
- http工具类 获取 请求中 的数据
```java
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * http 工具类 获取请求中的参数
 * @author show
 * @date 14:23 2019/5/29
 */
public class HttpUtils {
    /**
     * 将URL的参数和body参数合并
     * @author show
     * @date 14:24 2019/5/29
     * @param request
     */
    public static SortedMap<String, String> getAllParams(HttpServletRequest request) throws IOException {

        SortedMap<String, String> result = new TreeMap<>();
        //获取URL上的参数
        Map<String, String> urlParams = getUrlParams(request);
        for (Map.Entry entry : urlParams.entrySet()) {
            result.put((String) entry.getKey(), (String) entry.getValue());
        }
        Map<String, String> allRequestParam = new HashMap<>(16);
        // get请求不需要拿body参数
        if (!HttpMethod.GET.name().equals(request.getMethod())) {
            allRequestParam = getAllRequestParam(request);
        }
        //将URL的参数和body参数进行合并
        if (allRequestParam != null) {
            for (Map.Entry entry : allRequestParam.entrySet()) {
                result.put((String) entry.getKey(), (String) entry.getValue());
            }
        }
        return result;
    }

    /**
     * 获取 Body 参数
     * @author show
     * @date 15:04 2019/5/30
     * @param request
     */
    public static Map<String, String> getAllRequestParam(final HttpServletRequest request) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String str = "";
        StringBuilder wholeStr = new StringBuilder();
        //一行一行的读取body体里面的内容；
        while ((str = reader.readLine()) != null) {
            wholeStr.append(str);
        }
        //转化成json对象
        return JSONObject.parseObject(wholeStr.toString(), Map.class);
    }

    /**
     * 将URL请求参数转换成Map
     * @author show
     * @param request
     */
    public static Map<String, String> getUrlParams(HttpServletRequest request) {

        String param = "";
        try {
            param = URLDecoder.decode(request.getQueryString(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map<String, String> result = new HashMap<>(16);
        String[] params = param.split("&");
        for (String s : params) {
            int index = s.indexOf("=");
            result.put(s.substring(0, index), s.substring(index + 1));
        }
        return result;
    }
}
```


