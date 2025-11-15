package com.company.service.impl;

import com.company.client.AccountClient;
import com.company.common.BaseEvent;
import com.company.dao.entity.Payment;
import com.company.dao.repository.PaymentRepository;
import com.company.exception.AccountBlockedException;
import com.company.exception.InsufficientBalanceException;
import com.company.exception.NotFoundException;
import com.company.model.dto.AccountResponseDTO;
import com.company.model.dto.OrderDto;
import com.company.model.dto.request.DecreaseAccountRequest;
import com.company.model.dto.response.PaymentResponse;
import com.company.model.mapper.PaymentMapper;
import com.company.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.company.exception.constant.ErrorCode.ACCOUNT_BLOCKED;
import static com.company.exception.constant.ErrorCode.DATA_NOT_FOUND;
import static com.company.exception.constant.ErrorCode.INSUFFICIENT_BALANCE;
import static com.company.exception.constant.ErrorMessage.ACCOUNT_BLOCKED_MESSAGE;
import static com.company.exception.constant.ErrorMessage.DATA_NOT_FOUND_MESSAGE;
import static com.company.exception.constant.ErrorMessage.INSUFFICIENT_BALANCE_MESSAGE;
import static com.company.model.enums.PaymentStatus.SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final AccountClient accountClient;

    @Override
    public PaymentResponse getPaymentDetails(Long id, Long userId) {
        log.info("Getting payment details, paymentId {}, userId {}", id, userId);
        Payment payment = paymentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException(DATA_NOT_FOUND_MESSAGE, DATA_NOT_FOUND));

        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public void processPaymentRequest(BaseEvent<OrderDto> event) {
        Long userId = event.getPayload().getUserId();
        BigDecimal totalPrice = event.getPayload().getTotalAmount();
        Long orderId = event.getPayload().getId();
        log.info("Processing payment request. orderId {}, userId {}", orderId, userId);
        AccountResponseDTO accountResponse = accountClient.getAccountByUserId(userId);
        BigDecimal userBalance = accountResponse.getBalance();

        if ("BLOCKED".equals(accountResponse.getStatus())) {
            throw new AccountBlockedException(ACCOUNT_BLOCKED_MESSAGE, ACCOUNT_BLOCKED);
        }
        if (totalPrice.compareTo(userBalance) > 0) {
            throw new InsufficientBalanceException(INSUFFICIENT_BALANCE_MESSAGE, INSUFFICIENT_BALANCE);
        }
        DecreaseAccountRequest decreaseAccountRequest = DecreaseAccountRequest.builder()
                .balance(userBalance.subtract(totalPrice))
                .build();
        accountClient.updateUserBalance(userId, decreaseAccountRequest);
        Payment payment = createPaymentEntity(orderId, userId, totalPrice);
        paymentRepository.save(payment);
    }


    private Payment createPaymentEntity(Long orderId, Long userId, BigDecimal totalPrice) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setStatus(SUCCESS);
        payment.setUserId(userId);
        payment.setAmount(totalPrice);
        paymentRepository.save(payment);

        return payment;
    }

}
