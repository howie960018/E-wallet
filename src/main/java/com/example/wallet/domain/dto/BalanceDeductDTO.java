package com.example.wallet.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BalanceDeductDTO {

    @NotBlank(message = "使用者 ID 不能為空")
    private String userId;

    @NotBlank(message = "訂單號不能為空")
    private String orderId;

    @NotNull(message = "扣款金額不能為空")
    @Min(value = 1, message = "扣款金額最少為 1 分")
    private Long amount;

    /**
     * 扣款優先順序：
     * true  → 優先扣贈送金（accType=1），不足再扣現金（accType=0）
     * false → 直接扣現金
     */
    private boolean useGiftFirst = false;

    private String remark;
}
