package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户端订单相关接口 ")
@Slf4j
public class OrderController {

    @Autowired
    OrderService orderService;



    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单:{}",ordersSubmitDTO);
        OrderSubmitVO ordersSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(ordersSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);

        //模拟交易成功
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        log.info("模拟交易成功:{}",ordersPaymentDTO.getOrderNumber());

        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> payment(Integer page,Integer pageSize,Integer status){
        log.info("历史订单查询:{},{},{}",page,pageSize,status);
        PageResult pageResult = orderService.pageQuery(page,pageSize,status);
        return Result.success(pageResult);
    }

    /**
     * 订单详情查询
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("订单详情查询")
    public Result<OrderVO> payment(@PathVariable Long id){
        log.info("订单详情查询:{}",id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }


    /**
     * 取消订单
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id){
        log.info("取消订单:{}",id);
        orderService.cancelByUser(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id){
        log.info("再来一单:{}",id);
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 催单
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("客户催单")
    public Result reminder(@PathVariable Long id){
        log.info("客户催单:{}",id);
        orderService.reminder(id);
        return Result.success();
    }

}
