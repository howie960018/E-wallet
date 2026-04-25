package com.example.wallet.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BalanceRefundDTO {

    @NotBlank(message = "使用者 ID 不能為空")
    private String userId;

    /**
     * 原始扣款訂單號（退款依據）
     */
    @NotBlank(message = "原始訂單號不能為空")
    private String originalOrderId;

    /**
     * 新的退款訂單號（每次退款唯一）
     */
    @NotBlank(message = "退款訂單號不能為空")
    private String refundOrderId;

    @NotNull(message = "退款金額不能為空")
    @Min(value = 1, message = "退款金額最少為 1 分")
    private Long amount;
}