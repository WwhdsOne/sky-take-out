package com.sky.mapper;

import com.sky.entity.User;
import com.sky.vo.UserLoginVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {



    /**
     * 根据OpenId查找用户
     * @param openId
     * @return
     */
    @Select("select * from user where openid = #{openId}")
    User getByOpenId(String openId);

    /**
     * 插入新用户
     * @param user
     */
    void insert(User user);

    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * 根据动态数据统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
