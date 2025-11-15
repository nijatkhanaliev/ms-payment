package com.company.service;

import com.company.common.BaseEvent;
import com.company.model.dto.OrderDto;
import com.company.model.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse getPaymentDetails(Long id, Long userId);

    void processPaymentRequest(BaseEvent<OrderDto> event);

}
