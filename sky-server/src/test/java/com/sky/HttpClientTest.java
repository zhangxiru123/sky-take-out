package com.sky;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.http.HttpClient;

@SpringBootTest
public class HttpClientTest {
    @Test
    public void getTest() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:8080/user/shop/status");
        CloseableHttpResponse response = httpClient.execute(httpGet);

        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("服务器响应的状态码:"+statusCode);

        HttpEntity entity = response.getEntity();
        String string = EntityUtils.toString(entity);
        System.out.println("实体类对象:"+string);

        response.close();
        httpClient.close();
    }


    @Test
    public void testPost() throws IOException, JSONException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:8080/admin/employee/login");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username","admin");
        jsonObject.put("password","123456");

        StringEntity entity = new StringEntity(jsonObject.toString());
        entity.setContentEncoding("utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(httpPost);

        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("响应码为:"+statusCode);

        HttpEntity entity1 = response.getEntity();
        String string = EntityUtils.toString(entity1);
        System.out.println("响应数据为:"+string);

        response.close();
        httpClient.close();


    }
}
