package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.entity.User;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.pay.mock-enabled:false}")
    private boolean mockPayEnabled;

    @Value("${sky.shop.address:}")
    private String shopAddress;

    @Value("${sky.baidu.ak:}")
    private String baiduAk;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        checkOutOfRange(buildFullAddress(addressBook));

        Long currentId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(currentId).build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (CollectionUtils.isEmpty(list)) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayMethod(ordersSubmitDTO.getPayMethod() == 0 ? 1 : ordersSubmitDTO.getPayMethod());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(buildFullAddress(addressBook));
        orders.setUserId(currentId);

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        shoppingCartMapper.deleteByUserId(currentId);

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        if (mockPayEnabled) {
            log.warn("模拟支付已启用，订单 {} 将直接标记为支付成功", ordersPaymentDTO.getOrderNumber());
            Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
            if (ordersDB == null) {
                throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
            }
            if (Orders.PAID.equals(ordersDB.getPayStatus())) {
                throw new OrderBusinessException("该订单已支付");
            }
            markOrderPaid(ordersPaymentDTO.getOrderNumber(), ordersPaymentDTO.getPayMethod());
            OrderPaymentVO vo = new OrderPaymentVO();
            vo.setMock(true);
            return vo;
        }

        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(),
                new BigDecimal("0.01"),
                "苍穹外卖订单",
                user.getOpenid()
        );

        if ("ORDERPAID".equals(jsonObject.getString("code"))) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        return vo;
    }

    @Override
    public void paySuccess(String outTradeNo) {
        markOrderPaid(outTradeNo, null);


    }

    private void markOrderPaid(String outTradeNo, Integer payMethod) {
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .payMethod(payMethod)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);

        //通过websocket向客户端浏览器推送消息
        Map map=new HashMap();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号"+outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
        dto.setUserId(BaseContext.getCurrentId());
        dto.setStatus(status);
        Page<Orders> page = orderMapper.pageQuery(dto);
        if (page == null) {
            return new PageResult(0, new ArrayList<>());
        }
        List<OrderVO> list = new ArrayList<>();
        for (Orders orders : page) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(orders, vo);
            vo.setOrderDetailList(orderDetailMapper.getByOrderId(orders.getId()));
            list.add(vo);
        }
        return new PageResult(page.getTotal(), list);
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(orders, vo);
        vo.setOrderDetailList(orderDetailMapper.getByOrderId(id));
        return vo;
    }

    @Override
    @Transactional
    public void userCancelById(Long id) throws Exception {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (ordersDB.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();

        if (Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())
                && Orders.PAID.equals(ordersDB.getPayStatus())) {
            refundIfNecessary(ordersDB);
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);
    }

    @Override
    @Transactional
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        if (CollectionUtils.isEmpty(orderDetailList)) {
            return;
        }
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(detail -> {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(detail, cart, "id");
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            return cart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page == null ? 0 : page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> result = new ArrayList<>();
        if (page == null || CollectionUtils.isEmpty(page.getResult())) {
            return result;
        }
        for (Orders orders : page.getResult()) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(orders, vo);
            vo.setOrderDishes(getOrderDishesStr(orders));
            result.add(vo);
        }
        return result;
    }

    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> details = orderDetailMapper.getByOrderId(orders.getId());
        if (CollectionUtils.isEmpty(details)) {
            return "";
        }
        return details.stream()
                .map(item -> item.getName() + "*" + item.getNumber() + ";")
                .collect(Collectors.joining());
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setToBeConfirmed(defaultZero(orderMapper.countStatus(Orders.TO_BE_CONFIRMED)));
        vo.setConfirmed(defaultZero(orderMapper.countStatus(Orders.CONFIRMED)));
        vo.setDeliveryInProgress(defaultZero(orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS)));
        return vo;
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    @Transactional
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        if (ordersDB == null || !Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orderMapper.update(Orders.builder().id(ordersDB.getId()).status(Orders.CONFIRMED).build());
    }

    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if (ordersDB == null || !Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();

        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            refundIfNecessary(ordersDB);
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);
    }

    @Override
    @Transactional
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            refundIfNecessary(ordersDB);
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);
    }

    private void refundIfNecessary(Orders orders) throws Exception {
        if (mockPayEnabled) {
            log.warn("模拟支付已启用，跳过微信退款，订单号：{}", orders.getNumber());
            return;
        }
        String refund = weChatPayUtil.refund(
                orders.getNumber(),
                orders.getNumber(),
                new BigDecimal("0.01"),
                new BigDecimal("0.01"));
        log.info("申请退款：{}", refund);
    }

    @Override
    @Transactional
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !Orders.CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orderMapper.update(Orders.builder().id(id).status(Orders.DELIVERY_IN_PROGRESS).build());
    }

    @Override
    @Transactional
    public void complete(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !Orders.DELIVERY_IN_PROGRESS.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orderMapper.update(Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build());
    }

    /**
     * 客户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Arrays.asList(Orders.TO_BE_CONFIRMED, Orders.CONFIRMED, Orders.DELIVERY_IN_PROGRESS).contains(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map=new HashMap();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号:"+ordersDB.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    private String buildFullAddress(AddressBook addressBook) {
        return String.join("",
                valueOrEmpty(addressBook.getProvinceName()),
                valueOrEmpty(addressBook.getCityName()),
                valueOrEmpty(addressBook.getDistrictName()),
                valueOrEmpty(addressBook.getDetail()));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 未配置百度地图 AK 时跳过距离校验，避免开发环境无法下单。
     */
    private void checkOutOfRange(String address) {
        if (!StringUtils.hasText(baiduAk) || !StringUtils.hasText(shopAddress) || !StringUtils.hasText(address)) {
            log.warn("未配置 sky.baidu.ak 或 sky.shop.address，跳过配送范围校验");
            return;
        }

        Map<String, String> map = new HashMap<>();
        map.put("output", "json");
        map.put("ak", baiduAk);

        map.put("address", shopAddress);
        JSONObject shopJson = JSON.parseObject(HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map));
        if (shopJson == null || !"0".equals(shopJson.getString("status"))) {
            throw new OrderBusinessException("店铺地址解析失败");
        }
        JSONObject shopLocation = shopJson.getJSONObject("result").getJSONObject("location");
        String shopLngLat = shopLocation.getString("lat") + "," + shopLocation.getString("lng");

        map.put("address", address);
        JSONObject userJson = JSON.parseObject(HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map));
        if (userJson == null || !"0".equals(userJson.getString("status"))) {
            throw new OrderBusinessException("收货地址解析失败");
        }
        JSONObject userLocation = userJson.getJSONObject("result").getJSONObject("location");
        String userLngLat = userLocation.getString("lat") + "," + userLocation.getString("lng");

        map.remove("address");
        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        JSONObject routeJson = JSON.parseObject(HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map));
        if (routeJson == null || !"0".equals(routeJson.getString("status"))) {
            throw new OrderBusinessException("配送路线规划失败");
        }
        JSONArray routes = routeJson.getJSONObject("result").getJSONArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new OrderBusinessException("配送路线规划失败");
        }
        Integer distance = routes.getJSONObject(0).getInteger("distance");
        if (distance != null && distance > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }
}