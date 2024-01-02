package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);


    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);


    /**
     * 分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据ID查询订单
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);
    /**
     * 统计不同状态的订单数量
     * @param status
     * @return
     */
    @Select("select count(*) from orders where status = #{status}")
    Integer countByStatus(Integer status);

    /**
     * 根据订单状态和下单时间查询订单
     * @param status
     * @param orderDateTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderDateTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderDateTime);
}
