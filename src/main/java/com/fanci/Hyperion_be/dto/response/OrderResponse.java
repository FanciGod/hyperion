package com.fanci.Hyperion_be.dto.response;

import com.fanci.Hyperion_be.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String customerName;
    private String email;
    private String phoneNumber;
    private String address;
    private String note;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private String status;
    private String paymentUrl;
    private PaymentMethodResponse paymentMethodResponse;
    private List<OrderDetailResponse> orderDetailResponseList;

}
