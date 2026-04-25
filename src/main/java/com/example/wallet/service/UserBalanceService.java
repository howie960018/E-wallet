package com.example.wallet.service;


import com.example.wallet.common.result.PageResult;
import com.example.wallet.domain.bo.AccountBO;
import com.example.wallet.domain.bo.BalanceFlowBO;
import com.example.wallet.domain.dto.*;

public interface UserBalanceService {

    /**
     * 開戶
     */
    AccountBO openAccount(AccountOpenDTO dto);

    /**
     * 充值
     */
    void charge(BalanceChargeDTO dto);

    /**
     * 查詢帳戶餘額
     */
    AccountBO queryAccount(String userId, String accType);

    /**
     * 扣款（消費）
     */
    void deduct(BalanceDeductDTO dto);

    /**
     * 退款（將金額退回現金帳戶）
     */
    void refund(BalanceRefundDTO dto);

    /**
     * 查詢流水明細（分頁）
     */
    PageResult<BalanceFlowBO> queryFlow(FlowQueryDTO dto);
}