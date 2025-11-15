package com.company.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderDto {
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
}
