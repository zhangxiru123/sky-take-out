package com.sky.service.impl;

import com.sky.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 设置店铺状态
     * @param status
     */
    @Override
    public void setStatus(Integer status) {
         redisTemplate.opsForValue().set(KEY,status);

    }

    /**
     * 获取店铺营业状态
     * @return
     */
    @Override
    public Integer getStatus() {
       return (Integer) redisTemplate.opsForValue().get(KEY);
    }
}

