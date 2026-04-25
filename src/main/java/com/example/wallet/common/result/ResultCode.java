package com.example.wallet.common.result;


import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS("00000", "成功"),
    PARAM_ERROR("A0001", "參數錯誤"),
    ACCOUNT_EXIST("B0001", "帳戶已存在"),
    ACCOUNT_NOT_EXIST("B0002", "帳戶不存在"),
    BALANCE_NOT_ENOUGH("B0003", "餘額不足"),
    ORDER_EXIST("B0004", "訂單號已存在"),
    SYSTEM_ERROR("C0001", "系統內部錯誤"),
    DEDUCT_FAILED("B0005", "扣款失敗"),          // ← 加這行
    ORDER_NOT_EXIST("B0006", "訂單不存在"),       // ← 加這行
    ORDER_ALREADY_REFUNDED("B0007", "訂單已退款"); // ← 加這行（退款用）

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}