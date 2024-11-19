package com.fanci.Hyperion_be.service;

import com.fanci.Hyperion_be.dto.request.CreateNewOrderRequest;
import com.fanci.Hyperion_be.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.util.List;

public interface OrderService {
    Page<OrderResponse> findAllOrders(int page, int size);

    List<OrderResponse> findAllOrdersInCurrentMonth();

    OrderResponse findOrderById(Long id);

    OrderResponse addNewOrder(CreateNewOrderRequest request) throws IOException;


    OrderResponse setPendingAfterPaidById(Long id, String token);

    OrderResponse confirmOrderById(Long id);

    OrderResponse denyOrderById(Long id);
}
