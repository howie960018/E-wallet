package com.example.wallet.domain.bo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BalanceFlowBO implements Serializable {

    private Long id;

    private String userId;

    private String flowNo;

    private String accNo;

    /**
     * 變動金額（分）
     */
    private Long amount;

    private Long beginBalance;

    private Long endBalance;

    /**
     * 00-支出；01-收入
     */
    private String fundDirect;

    /**
     * 方向中文描述（方便前端顯示）
     */
    private String fundDirectDesc;

    private LocalDateTime createTime;
}