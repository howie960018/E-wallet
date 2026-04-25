package com.example.wallet.domain.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GiftBalanceGrantDTO {

    @NotBlank(message = "使用者 ID 不能為空")
    private String userId;

    @NotBlank(message = "發放訂單號不能為空")
    private String orderId;

    @NotNull(message = "發放金額不能為空")
    @Min(value = 1, message = "發放金額最少為 1 分")
    private Long amount;

    /**
     * 發放原因（例如：註冊禮、活動獎勵）
     */
    private String remark;
}