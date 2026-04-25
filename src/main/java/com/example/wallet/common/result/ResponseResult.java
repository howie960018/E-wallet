package com.example.wallet.common.result;


import lombok.Data;

@Data
public class ResponseResult<T> {

    private String code;
    private String message;
    private T data;

    // 成功（有資料）
    public static <T> ResponseResult<T> success(T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    // 成功（無資料）
    public static <T> ResponseResult<T> success() {
        return success(null);
    }

    // 失敗
    public static <T> ResponseResult<T> fail(ResultCode resultCode) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    // 失敗（自訂訊息）
    public static <T> ResponseResult<T> fail(ResultCode resultCode, String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(resultCode.getCode());
        result.setMessage(message);
        return result;
    }
}