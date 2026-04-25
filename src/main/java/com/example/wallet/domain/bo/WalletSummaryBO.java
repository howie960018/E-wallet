package com.example.wallet.domain.bo;


import lombok.Data;
import java.io.Serializable;

@Data
public class WalletSummaryBO implements Serializable {

    private String userId;

    /**
     * 現金帳戶餘額（分）
     */
    private Long cashBalance;

    private String cashAccNo;

    /**
     * 贈送金帳戶餘額（分）
     */
    private Long giftBalance;

    private String giftAccNo;

    /**
     * 總可用餘額（現金 + 贈送金）
     */
    private Long totalBalance;
}