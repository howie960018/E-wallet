package com.example.wallet.domain.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountOpenDTO {

    @NotBlank(message = "使用者 ID 不能為空")
    private String userId;

    /**
     * 帳戶類型：0-現金；1-贈送金
     */
    @NotNull(message = "帳戶類型不能為空")
    private String accType;
}