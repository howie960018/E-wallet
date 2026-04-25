package com.example.wallet.controller;


import com.example.wallet.common.result.PageResult;
import com.example.wallet.common.result.ResponseResult;
import com.example.wallet.domain.bo.AccountBO;
import com.example.wallet.domain.bo.BalanceFlowBO;
import com.example.wallet.domain.bo.WalletSummaryBO;
import com.example.wallet.domain.dto.*;
import com.example.wallet.service.UserBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class UserBalanceController {

    private final UserBalanceService userBalanceService;

    /**
     * 開戶
     * POST /api/wallet/account/open
     */
    @PostMapping("/account/open")
    public ResponseResult<AccountBO> openAccount(
            @RequestBody @Validated AccountOpenDTO dto) {
        log.info("開戶請求，userId={}, accType={}", dto.getUserId(), dto.getAccType());
        AccountBO result = userBalanceService.openAccount(dto);
        return ResponseResult.success(result);
    }

    /**
     * 充值
     * POST /api/wallet/charge
     */
    @PostMapping("/charge")
    public ResponseResult<Void> charge(
            @RequestBody @Validated BalanceChargeDTO dto) {
        log.info("充值請求，userId={}, orderId={}, amount={}",
                dto.getUserId(), dto.getOrderId(), dto.getAmount());
        userBalanceService.charge(dto);
        return ResponseResult.success();
    }

    /**
     * 查詢帳戶餘額
     * GET /api/wallet/account?userId=xxx&accType=0
     */
    @GetMapping("/account")
    public ResponseResult<AccountBO> queryAccount(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") String accType) {
        log.info("查詢帳戶請求，userId={}, accType={}", userId, accType);
        AccountBO result = userBalanceService.queryAccount(userId, accType);
        return ResponseResult.success(result);
    }


    /**
     * 扣款（消費）
     * POST /api/wallet/deduct
     */
    @PostMapping("/deduct")
    public ResponseResult<Void> deduct(
            @RequestBody @Validated BalanceDeductDTO dto) {
        log.info("扣款請求，userId={}, orderId={}, amount={}, useGiftFirst={}",
                dto.getUserId(), dto.getOrderId(), dto.getAmount(), dto.isUseGiftFirst());
        userBalanceService.deduct(dto);
        return ResponseResult.success();
    }

    /**
     * 退款
     * POST /api/wallet/refund
     */
    @PostMapping("/refund")
    public ResponseResult<Void> refund(
            @RequestBody @Validated BalanceRefundDTO dto) {
        log.info("退款請求，userId={}, originalOrderId={}, refundOrderId={}, amount={}",
                dto.getUserId(), dto.getOriginalOrderId(),
                dto.getRefundOrderId(), dto.getAmount());
        userBalanceService.refund(dto);
        return ResponseResult.success();
    }


    /**
     * 查詢流水明細（分頁）
     * GET /api/wallet/flow?userId=xxx&pageNum=1&pageSize=10
     */
    @GetMapping("/flow")
    public ResponseResult<PageResult<BalanceFlowBO>> queryFlow(FlowQueryDTO dto) {
        log.info("查詢流水請求，userId={}, pageNum={}, pageSize={}",
                dto.getUserId(), dto.getPageNum(), dto.getPageSize());
        PageResult<BalanceFlowBO> result = userBalanceService.queryFlow(dto);
        return ResponseResult.success(result);
    }


    /**
     * 發放贈送金
     * POST /api/wallet/gift/grant
     */
    @PostMapping("/gift/grant")
    public ResponseResult<Void> grantGift(
            @RequestBody @Validated GiftBalanceGrantDTO dto) {
        log.info("發放贈送金請求，userId={}, orderId={}, amount={}",
                dto.getUserId(), dto.getOrderId(), dto.getAmount());
        userBalanceService.grantGift(dto);
        return ResponseResult.success();
    }

    /**
     * 查詢錢包總覽
     * GET /api/wallet/summary?userId=xxx
     */
    @GetMapping("/summary")
    public ResponseResult<WalletSummaryBO> queryWalletSummary(
            @RequestParam String userId) {
        log.info("查詢錢包總覽，userId={}", userId);
        WalletSummaryBO result = userBalanceService.queryWalletSummary(userId);
        return ResponseResult.success(result);
    }
}