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
![成功示例图](https://upload-images.jianshu.io/upload_images/13183199-e0af7e0a7ee986d9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
![失败示例图](https://upload-images.jianshu.io/upload_images/13183199-a26affcf8a3eee62.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
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
   1.不管GET Url 还是 POST Body 的参数，都转换成 json 对象，用 ascll码排序 对参数排序。
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


