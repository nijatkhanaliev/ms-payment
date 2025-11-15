package com.company.service.impl;

import com.company.common.BaseEvent;
import com.company.common.BaseResultEvent;
import com.company.exception.AccountBlockedException;
import com.company.exception.InsufficientBalanceException;
import com.company.messaging.MessageProducer;
import com.company.model.dto.OrderDto;
import com.company.model.dto.ResponseQueueInfo;
import com.company.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final PaymentService paymentService;
    private final MessageProducer messageProducer;

    public void processOrderCreated(BaseEvent<OrderDto> event) {
        log.info("Process order created. eventId {}", event.getEventId());
        Long orderId = event.getPayload().getId();
        ResponseQueueInfo responseQueueInfo = event.getResponseQueueInfo();
        final String responseExchange = responseQueueInfo.getExchange();
        final String responseRTK = responseQueueInfo.getRoutingKey();
        try {
            paymentService.processPaymentRequest(event);
            BaseResultEvent resultEvent = createResultEvent(event.getEventId(),
                    orderId, "SUCCESS", null);
            messageProducer.sendOrderCreatedResult(responseExchange, responseRTK, resultEvent);
        } catch (AccountBlockedException | InsufficientBalanceException ex) {
            log.error("Exception happened during PAYMENT.ORDER.CREATED: {}", ex.getMessage(), ex);
            BaseResultEvent resultEvent = createResultEvent(event.getEventId(),
                    orderId, "FAILED", ex.getMessage());
            messageProducer.sendOrderCreatedResult(responseExchange, responseRTK, resultEvent);
        } catch (Exception ex) {
            log.error("Unexpected exception during PAYMENT.ORDER.CREATED: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    private BaseResultEvent createResultEvent(String eventId, Long orderId, String status, String message) {
        return BaseResultEvent.builder()
                .eventId(eventId)
                .status(status)
                .orderId(orderId)
                .reason(message)
                .build();
    }

}
