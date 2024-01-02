package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")//每分钟一次
    public void processTimeoutOrder(){
        log.info("定时处理超时订单:{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.UN_PAID, time);
        if(orders != null && !orders.isEmpty()){
            for ( Orders order : orders ) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 定时处理处于派送中订单
     */
    @Scheduled(cron = "0 * 1 * * ?")//每天凌晨一点执行
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中订单:{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().minusHours(1);
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if(orders != null && !orders.isEmpty()){
            for ( Orders order : orders ) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}