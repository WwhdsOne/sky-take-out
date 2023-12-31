package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 获取订单餐品构成的字符串
     * @param orders
     * @return
     */
    private String getDishesString(Orders orders){
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        StringBuilder stringBuilder = new StringBuilder();
        for ( OrderDetail orderDetail : orderDetails ) {
            stringBuilder.append(orderDetail.getName()).append("*")
                    .append(orderDetail.getNumber()).append(";");
        }
        return stringBuilder.toString();
    }

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理各种异常(地址簿为空,购物车为空)
        //查询当前用户地址
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if ( addressBook == null ) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //查询当前用户购物车
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if ( shoppingCartList == null || shoppingCartList.isEmpty() ) {
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条订单数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getProvinceName() +
                        addressBook.getCityName()+
                        addressBook.getDistrictName() +
                        addressBook.getDetail()
                          );
        //收货人
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);
        //向订单明细表插入多条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for ( ShoppingCart cart : shoppingCartList ) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);
        //清空当前用户车购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //返回封装VO结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }


    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult pageQuery(Integer page,Integer pageSize,Integer status) {
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setPage(page);
        ordersPageQueryDTO.setPageSize(pageSize);
        ordersPageQueryDTO.setStatus(status);
        //分页查询
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> pageOrder = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if(pageOrder != null && !pageOrder.isEmpty()){
            for ( Orders orders : pageOrder ) {
                //获取订单细节
                Long orderId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                //封装VO对象
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(pageOrder.getTotal(),list);
    }

    /**
     * 订单详情查询
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        OrderVO orderVO = new OrderVO();
        Orders orders = orderMapper.getById(id);
        BeanUtils.copyProperties(orders,orderVO);
        List<OrderDetail> byOrderId = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(byOrderId);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     * @return
     */
    @Override
    public void cancelByUser(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Integer status = orderDB.getStatus();
        if(status > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        if( status.equals(Orders.TO_BE_CONFIRMED) ){
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param orderId
     * @return
     */
    @Override

    public void repetition(Long orderId) {
        //将此单所有商品重新添加至购物车
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

        List<ShoppingCart> shoppingCartList = orderDetails.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 催单
     * @param id
     * @return
     */
    @Override
    public void reminder(Long id) {

    }

    /**
     * 订单搜索
     * @return
     */
    @Override
    public PageResult conditionalSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);
        List<Orders> result = orders.getResult();
        List<OrderVO> orderVOList = new ArrayList<>();
        for ( Orders order : result ) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order,orderVO);
            orderVO.setOrderDishes(getDishesString(order));
            orderVOList.add(orderVO);
        }
        return new PageResult(orders.getTotal(),orderVOList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        //获取待接单订单数量
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        //获取待派送订单数量
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        //获取派送中订单数量
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     * @return
     */
    @Override
    public void confirmOrder(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     * @return
     */
    @Override
    public void rejectOrder(OrdersRejectionDTO ordersRejectionDTO) {
        //判断其是否为空或状态不为待接单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if(ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.TO_BE_CONFIRMED) ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //模拟用户退款
        log.info("退款成功");
        //拒单操作
        Orders orders = Orders.builder()
                .status(Orders.CANCELLED)
                .id(ordersRejectionDTO.getId())
                .cancelReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @Override
    public void deliveryOrder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.CONFIRMED) ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     * @return
     */
    @Override
    public void completeOrder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.DELIVERY_IN_PROGRESS) ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     * @return
     */
    @Override
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) {
        Orders orderDB = orderMapper.getById(ordersCancelDTO.getId());

        //当订单未完成时可取消
        Integer status = orderDB.getStatus();
        if(status < Orders.COMPLETED && status > Orders.UN_PAID){
            log.info("模拟退款");
        }
        //设置取消订单细节
        Orders orders = Orders.builder()
                .id(orderDB.getId())
                .cancelTime(LocalDateTime.now())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .build();
        orderMapper.update(orders);
    }


}
