package com.fanci.Hyperion_be.service.serviceImpl;

import com.fanci.Hyperion_be.dto.request.CreateNewOrderRequest;
import com.fanci.Hyperion_be.dto.response.OrderDetailResponse;
import com.fanci.Hyperion_be.dto.response.OrderResponse;
import com.fanci.Hyperion_be.entity.Orders;
import com.fanci.Hyperion_be.entity.OrderDetail;
import com.fanci.Hyperion_be.entity.ProductDetail;
import com.fanci.Hyperion_be.enums.OrderStatus;
import com.fanci.Hyperion_be.exception.AppException;
import com.fanci.Hyperion_be.exception.ErrorCode;
import com.fanci.Hyperion_be.mapper.OrderDetailMapper;
import com.fanci.Hyperion_be.mapper.OrderMapper;
import com.fanci.Hyperion_be.mapper.PaymentMethodMapper;
import com.fanci.Hyperion_be.repository.OrderRepository;
import com.fanci.Hyperion_be.repository.PaymentMethodRepository;
import com.fanci.Hyperion_be.repository.ProductDetailRepository;
import com.fanci.Hyperion_be.service.OrderService;
import com.fanci.Hyperion_be.utility.MailContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ProductDetailRepository productDetailRepository;

    private final NotificationServiceImpl notificationServiceImpl;

    private final PaypalService paypalService;

    private final ProductDetailServiceImpl productDetailServiceImpl;

    private final OrderMapper orderMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final OrderDetailMapper orderDetailMapper;


    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR','SALE')")
    public Page<OrderResponse> findAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return orderRepository.findAll(pageable).map(this::toOrderResponse);
    }
    @Override
    public List<OrderResponse> findAllOrdersInCurrentMonth(){
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(23, 59, 59);
    return orderRepository.findOrdersInMonth(startOfMonth,endOfMonth).stream().map(this::toOrderResponse).toList();
    }

    @Override
    public OrderResponse findOrderById(Long id) {
        Orders orders = orderRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ORDER_ID_NOT_FOUND));
        return toOrderResponse(orders);
    }

    @Override
    @Transactional
    public OrderResponse addNewOrder(CreateNewOrderRequest request) throws IOException {
        Orders rawOrders = orderMapper.toOrder(request);
        rawOrders.setCreatedAt(LocalDateTime.now());
        rawOrders.setPaymentMethod(paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_ID_NOT_FOUND)));


        List<ProductDetail> updatedProductDetails = new ArrayList<>();

        List<OrderDetail> orderDetailList = request.getCreateNewOrderDetailDtoList().stream().map(dto -> {
            ProductDetail productDetail = productDetailRepository.findById(dto.getProductDetailId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_DETAIL_ID_NOT_FOUND));
            if (productDetail.getStock() < dto.getAmount()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK, productDetail.getProduct().getName());
            }
            updatedProductDetails.add(productDetail);
            return OrderDetail.builder()
                    .productDetail(productDetail)
                    .amount(dto.getAmount())
                    .orders(rawOrders)
                    .build();
        }).toList();
        productDetailRepository.saveAll(updatedProductDetails);
        rawOrders.setOrderDetails(orderDetailList);


        if (request.getPaymentMethodId() == 1) {
            rawOrders.setStatus(OrderStatus.PENDING);
            var order = orderRepository.save(rawOrders);
            notificationServiceImpl.createNewNotificationWhenCreateNewOrder(order);
            notificationServiceImpl.sendOrderMail(order.getEmail(), MailContent.pendingSubject, MailContent.htmlPendingContent, order.getId());
            notificationServiceImpl.sendNotificationByWebSocket(order);



            return toOrderResponse(order);
        }

        if (request.getPaymentMethodId() == 2) {
            rawOrders.setStatus(OrderStatus.AWAITING_PAYMENT);
            var order = orderRepository.save(rawOrders);
            notificationServiceImpl.createNewNotificationWhenCreateNewOrder(order);
            notificationServiceImpl.sendNotificationByWebSocket(order);
            double totalPrice = 0D;
            for (OrderDetail orderDetail : orderDetailList) {
                totalPrice = totalPrice + orderDetail.getProductDetail().getProduct().getPrice() * orderDetail.getAmount();
            }
            var orderResponse = toOrderResponse(order);
            orderResponse.setPaymentUrl(paypalService.createOrder(totalPrice, order.getId()));


            return orderResponse;

        }


        return null;

    }

    @Override
    public OrderResponse setPendingAfterPaidById(Long id, String token) {
        if (paypalService.checkPaymentStatus(token)) {
            Orders order = orderRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ORDER_ID_NOT_FOUND));
            order.setStatus(OrderStatus.PENDING);
            notificationServiceImpl.sendOrderMail(order.getEmail(), MailContent.pendingSubject, MailContent.htmlPendingContent, order.getId());
            notificationServiceImpl.sendNotificationByWebSocket(order);
            notificationServiceImpl.createNewNotificationWhenCreateNewOrder(order);
            return toOrderResponse(orderRepository.save(order));

        }
        throw new AppException(ErrorCode.PAYPAL_TOKEN_DENIED);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SALE')")
    public OrderResponse confirmOrderById(Long id) {
        Orders order = orderRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ORDER_ID_NOT_FOUND));
        order.setStatus(OrderStatus.CONFIRMED);

        List<ProductDetail> updatedProductDetails = order.getOrderDetails().stream()
                .map(orderDetail -> {
                    ProductDetail productDetail = orderDetail.getProductDetail();
                    if (productDetail.getStock() < orderDetail.getAmount()) {
                        throw new AppException(ErrorCode.OUT_OF_STOCK, productDetail.getProduct().getName());
                    }
                    productDetail.setStock(productDetail.getStock() - orderDetail.getAmount());
                    return productDetail;
                })
                .toList();
        productDetailRepository.saveAll(updatedProductDetails);
        notificationServiceImpl.createNewNotificationWhenCreateNewOrder(order);
        notificationServiceImpl.sendOrderMail(order.getEmail(), MailContent.confirmedSubject, MailContent.htmlPendingContent, order.getId());
        notificationServiceImpl.sendNotificationByWebSocket(order);
        return toOrderResponse(orderRepository.save(order));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SALE')")
    public OrderResponse denyOrderById(Long id) {
        Orders order = orderRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ORDER_ID_NOT_FOUND));
        order.setStatus(OrderStatus.DENIED);
        notificationServiceImpl.createNewNotificationWhenCreateNewOrder(order);
        notificationServiceImpl.sendOrderMail(order.getEmail(), MailContent.deniedSubject, MailContent.htmlPendingContent, order.getId());
        notificationServiceImpl.sendNotificationByWebSocket(order);
        return toOrderResponse(orderRepository.save(order));
    }

    private OrderResponse toOrderResponse(Orders orders) {
        OrderResponse orderResponse = orderMapper.toOrderResponse(orders);
        orderResponse.setStatus(orders.getStatus().toString());
        orderResponse.setPaymentMethodResponse(paymentMethodMapper.toPaymentMethodResponse(orders.getPaymentMethod()));
        orderResponse.setOrderDetailResponseList(orders.getOrderDetails().stream().map(this::toOrderDetailResponse).toList());
        return orderResponse;
    }

    private OrderDetailResponse toOrderDetailResponse(OrderDetail orderDetail) {
        OrderDetailResponse orderDetailResponse = orderDetailMapper.toOrderDetailResponse(orderDetail);
        orderDetailResponse.setProductDetailResponse(productDetailServiceImpl.toProductDetailResponse(orderDetail.getProductDetail()));
        return orderDetailResponse;
    }


}
