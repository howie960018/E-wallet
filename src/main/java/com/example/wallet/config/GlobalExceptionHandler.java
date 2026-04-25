package com.example.wallet.config;


import com.example.wallet.common.result.BizException;
import com.example.wallet.common.result.ResponseResult;
import com.example.wallet.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 處理參數校驗失敗（@Validated 觸發）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<?> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = bindingResult.getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + "：" + fe.getDefaultMessage())
                .findFirst()
                .orElse("參數錯誤");
        log.warn("參數校驗失敗：{}", message);
        return ResponseResult.fail(ResultCode.PARAM_ERROR, message);
    }

    // 處理業務例外
    @ExceptionHandler(BizException.class)
    public ResponseResult<?> handleBizException(BizException e) {
        log.warn("業務異常：{}", e.getMessage());
        return ResponseResult.fail(e.getResultCode());
    }

    // 處理其他未預期例外
    @ExceptionHandler(Exception.class)
    public ResponseResult<?> handleException(Exception e) {
        log.error("系統異常：", e);
        return ResponseResult.fail(ResultCode.SYSTEM_ERROR);
    }
}