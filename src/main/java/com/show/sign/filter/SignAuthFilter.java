package com.show.sign.filter;

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


