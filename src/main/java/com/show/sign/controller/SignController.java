package com.show.sign.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 签名测试
 * @author show
 * @date 10:53 2019/5/30
 */
@RestController
@Slf4j
@RequestMapping("/signTest")
public class SignController {

    @PostMapping
    public Map<String, Object> signTestPost(@RequestBody JSONObject user) {

        String username = (String) user.get("username");
        String password = (String) user.get("username");
        log.info("username：{},password：{}", username, password);
        Map<String, Object> resParam = new HashMap<>(16);
        resParam.put("msg", "参数校验成功");
        resParam.put("success", "true");
        return resParam;
    }

    @GetMapping
    public Map<String, Object> signTestGet(String username, String password) {

        log.info("username：{},password：{}", username, password);
        Map<String, Object> resParam = new HashMap<>(16);
        resParam.put("msg", "参数校验成功");
        resParam.put("success", "true");
        return resParam;
    }

}
