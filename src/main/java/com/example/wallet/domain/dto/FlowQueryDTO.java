package com.example.wallet.domain.dto;


import lombok.Data;

@Data
public class FlowQueryDTO {

    private String userId;

    private String accNo;

    /**
     * 借貸方向篩選：00-支出；01-收入；null-全部
     */
    private String fundDirect;

    /**
     * 頁碼，從 1 開始
     */
    private Integer pageNum = 1;

    /**
     * 每頁筆數，預設 10
     */
    private Integer pageSize = 10;
}