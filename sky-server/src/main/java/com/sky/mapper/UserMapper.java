package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid来查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid=#{openid}")
    User getByOpenid(String openid);

    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    @Select("select * from user where id=#{id}")
    User getById(Long id);

    void insert(User user);

    /**
     * 根据时间区间查询
     * @param begin
     * @param end
     * @return
     */
    Integer countByTime(LocalDateTime begin,LocalDateTime end);

}
