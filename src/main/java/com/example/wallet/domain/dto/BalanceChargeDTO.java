package com.example.wallet.domain.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BalanceChargeDTO {

    @NotBlank(message = "使用者 ID 不能為空")
    private String userId;

    @NotBlank(message = "訂單號不能為空")
    private String orderId;

    @NotNull(message = "充值金額不能為空")
    @Min(value = 1, message = "充值金額最少為 1 分")
    private Long amount;

    /**
     * 支付通路流水號（支付完成後由支付方回傳）
     */
    private String tradeNo;
}