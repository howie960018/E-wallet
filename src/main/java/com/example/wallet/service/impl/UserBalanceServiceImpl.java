package com.example.wallet.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.wallet.common.result.BizException;
import com.example.wallet.common.result.PageResult;
import com.example.wallet.common.result.ResultCode;
import com.example.wallet.domain.bo.AccountBO;
import com.example.wallet.domain.bo.BalanceFlowBO;
import com.example.wallet.domain.bo.UserBalanceConvert;
import com.example.wallet.domain.bo.WalletSummaryBO;
import com.example.wallet.domain.dto.*;
import com.example.wallet.domain.po.UserBalanceFlowPO;
import com.example.wallet.domain.po.UserBalanceOrderPO;
import com.example.wallet.domain.po.UserBalancePO;
import com.example.wallet.manager.UserBalanceManager;
import com.example.wallet.service.UserBalanceService;
import com.example.wallet.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBalanceServiceImpl implements UserBalanceService {

    private final UserBalanceManager userBalanceManager;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    // ===================== 開戶 =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountBO openAccount(AccountOpenDTO dto) {
        UserBalancePO existing = userBalanceManager
                .getByUserIdAndType(dto.getUserId(), dto.getAccType());
        if (existing != null) {
            throw new BizException(ResultCode.ACCOUNT_EXIST);
        }

        UserBalancePO po = new UserBalancePO();
        po.setUserId(dto.getUserId());
        po.setAccNo(generateAccNo(dto.getUserId(), dto.getAccType()));
        po.setAccType(dto.getAccType());
        po.setBalance(0L);

        userBalanceManager.saveAccount(po);
        log.info("開戶成功，userId={}, accNo={}", dto.getUserId(), po.getAccNo());

        return UserBalanceConvert.INSTANCE.convertToBO(po);
    }

    // ===================== 充值 =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void charge(BalanceChargeDTO dto) {
        UserBalanceOrderPO existingOrder = userBalanceManager
                .getByOrderId(dto.getOrderId());
        if (existingOrder != null) {
            if ("2".equals(existingOrder.getStatus())) {
                log.warn("訂單已處理過，orderId={}", dto.getOrderId());
                return;
            }
            throw new BizException(ResultCode.ORDER_EXIST);
        }

        UserBalancePO account = userBalanceManager
                .getByUserIdAndType(dto.getUserId(), "0");
        if (account == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_EXIST);
        }

        UserBalanceOrderPO order = new UserBalanceOrderPO();
        order.setOrderId(dto.getOrderId());
        order.setUserId(dto.getUserId());
        order.setAmount(dto.getAmount());
        order.setTradeType("charge");
        order.setTradeNo(dto.getTradeNo());
        order.setStatus("1");
        order.setIsRenew(0);
        userBalanceManager.saveOrder(order);

        long beginBalance = account.getBalance();
        long endBalance = beginBalance + dto.getAmount();
        int rows = userBalanceManager.updateBalance(account, endBalance);
        if (rows == 0) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        userBalanceManager.updateOrderStatus(dto.getOrderId(), "2", dto.getTradeNo());

        UserBalanceFlowPO flow = new UserBalanceFlowPO();
        flow.setUserId(dto.getUserId());
        flow.setFlowNo(snowflakeIdGenerator.nextFlowNo());
        flow.setAccNo(account.getAccNo());
        flow.setAmount(dto.getAmount());
        flow.setBeginBalance(beginBalance);
        flow.setEndBalance(endBalance);
        flow.setFundDirect("01");
        userBalanceManager.saveFlow(flow);

        log.info("充值成功，userId={}, orderId={}, amount={}, endBalance={}",
                dto.getUserId(), dto.getOrderId(), dto.getAmount(), endBalance);
    }

    // ===================== 扣款 =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(BalanceDeductDTO dto) {
        // 1. 冪等保護
        UserBalanceOrderPO existingOrder = userBalanceManager
                .getByOrderId(dto.getOrderId());
        if (existingOrder != null) {
            if ("2".equals(existingOrder.getStatus())) {
                log.warn("扣款訂單已處理過，orderId={}", dto.getOrderId());
                return;
            }
            throw new BizException(ResultCode.ORDER_EXIST);
        }

        // 2. 查詢所有帳戶
        List<UserBalancePO> accounts = userBalanceManager
                .listByUserId(dto.getUserId());
        if (accounts.isEmpty()) {
            throw new BizException(ResultCode.ACCOUNT_NOT_EXIST);
        }

        UserBalancePO cashAccount = accounts.stream()
                .filter(a -> "0".equals(a.getAccType()))
                .findFirst().orElse(null);
        UserBalancePO giftAccount = accounts.stream()
                .filter(a -> "1".equals(a.getAccType()))
                .findFirst().orElse(null);

        long totalBalance = (cashAccount != null ? cashAccount.getBalance() : 0L)
                + (giftAccount != null ? giftAccount.getBalance() : 0L);

        // 3. 檢查總餘額
        if (totalBalance < dto.getAmount()) {
            throw new BizException(ResultCode.BALANCE_NOT_ENOUGH);
        }

        // 4. 建立訂單
        UserBalanceOrderPO order = new UserBalanceOrderPO();
        order.setOrderId(dto.getOrderId());
        order.setUserId(dto.getUserId());
        order.setAmount(dto.getAmount());
        order.setTradeType("deduct");
        order.setStatus("1");
        order.setIsRenew(0);
        userBalanceManager.saveOrder(order);

        // 5. 執行扣款
        long remaining = dto.getAmount();

        if (dto.isUseGiftFirst() && giftAccount != null && giftAccount.getBalance() > 0) {
            long giftDeduct = Math.min(giftAccount.getBalance(), remaining);
            remaining -= giftDeduct;
            updateBalanceAndWriteFlow(giftAccount, -giftDeduct, "00");
        }

        if (remaining > 0 && cashAccount != null) {
            if (cashAccount.getBalance() < remaining) {
                throw new BizException(ResultCode.BALANCE_NOT_ENOUGH);
            }
            updateBalanceAndWriteFlow(cashAccount, -remaining, "00");
        } else if (remaining > 0) {
            throw new BizException(ResultCode.BALANCE_NOT_ENOUGH);
        }

        // 6. 訂單完成
        userBalanceManager.updateOrderStatus(dto.getOrderId(), "2", null);

        log.info("扣款成功，userId={}, orderId={}, amount={}",
                dto.getUserId(), dto.getOrderId(), dto.getAmount());
    }

    // ===================== 查詢 =====================

    @Override
    public AccountBO queryAccount(String userId, String accType) {
        UserBalancePO po = userBalanceManager
                .getByUserIdAndType(userId, accType);
        if (po == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_EXIST);
        }
        return UserBalanceConvert.INSTANCE.convertToBO(po);
    }

    // ===================== 私有方法 =====================

    private String generateAccNo(String userId, String accType) {
        String prefix = userId.length() >= 8
                ? userId.substring(0, 8)
                : userId;
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        return "ACC" + prefix + accType + random;
    }


    private void updateBalanceAndWriteFlow(UserBalancePO account,
                                           long delta,
                                           String fundDirect) {
        long beginBalance = account.getBalance();
        long endBalance = beginBalance + delta;

        // 樂觀鎖更新，updateById 會自動帶 version 條件
        int rows = userBalanceManager.updateBalance(account, endBalance);

        // rows = 0 代表 version 不符，其他請求已先更新
        if (rows == 0) {
            throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
        }

        UserBalanceFlowPO flow = new UserBalanceFlowPO();
        flow.setUserId(account.getUserId());
        flow.setFlowNo(snowflakeIdGenerator.nextFlowNo());
        flow.setAccNo(account.getAccNo());
        flow.setAmount(Math.abs(delta));
        flow.setBeginBalance(beginBalance);
        flow.setEndBalance(endBalance);
        flow.setFundDirect(fundDirect);
        userBalanceManager.saveFlow(flow);

        account.setBalance(endBalance);
    }

    // ===================== 退款 =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(BalanceRefundDTO dto) {
        // 1. 冪等保護：退款訂單不能重複
        UserBalanceOrderPO existingRefund = userBalanceManager
                .getByOrderId(dto.getRefundOrderId());
        if (existingRefund != null) {
            if ("2".equals(existingRefund.getStatus())) {
                log.warn("退款訂單已處理過，refundOrderId={}", dto.getRefundOrderId());
                return;
            }
            throw new BizException(ResultCode.ORDER_EXIST);
        }

        // 2. 查詢原始扣款訂單，確認存在且已成功
        UserBalanceOrderPO originalOrder = userBalanceManager
                .getByOrderId(dto.getOriginalOrderId());
        if (originalOrder == null) {
            throw new BizException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!"2".equals(originalOrder.getStatus())) {
            throw new BizException(ResultCode.DEDUCT_FAILED);
        }
        if ("refund".equals(originalOrder.getTradeType())) {
            throw new BizException(ResultCode.ORDER_ALREADY_REFUNDED);
        }

        // 3. 確認退款金額不超過原始扣款金額
        if (dto.getAmount() > originalOrder.getAmount()) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }

        // 4. 查詢現金帳戶（退款固定退回現金帳戶）
        UserBalancePO cashAccount = userBalanceManager
                .getByUserIdAndType(dto.getUserId(), "0");
        if (cashAccount == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_EXIST);
        }

        // 5. 建立退款訂單
        UserBalanceOrderPO refundOrder = new UserBalanceOrderPO();
        refundOrder.setOrderId(dto.getRefundOrderId());
        refundOrder.setUserId(dto.getUserId());
        refundOrder.setAmount(dto.getAmount());
        refundOrder.setTradeType("refund");
        refundOrder.setStatus("1");
        refundOrder.setIsRenew(0);
        userBalanceManager.saveOrder(refundOrder);

        // 6. 退款金額加回現金帳戶，並寫流水
        updateBalanceAndWriteFlow(cashAccount, dto.getAmount(), "01");

        // 7. 退款訂單完成，同時將原始訂單標記為已退款
        userBalanceManager.updateOrderStatus(dto.getRefundOrderId(), "2", null);
        userBalanceManager.updateOrderTradeType(dto.getOriginalOrderId(), "refund");

        log.info("退款成功，userId={}, originalOrderId={}, refundOrderId={}, amount={}",
                dto.getUserId(), dto.getOriginalOrderId(),
                dto.getRefundOrderId(), dto.getAmount());
    }


    // ===================== 查詢流水 =====================

    @Override
    public PageResult<BalanceFlowBO> queryFlow(FlowQueryDTO dto) {
        IPage<UserBalanceFlowPO> page = userBalanceManager.pageFlow(
                dto.getUserId(),
                dto.getAccNo(),
                dto.getFundDirect(),
                dto.getPageNum(),
                dto.getPageSize()
        );

        // PO 轉 BO，並補上中文描述
        List<BalanceFlowBO> records = UserBalanceConvert.INSTANCE
                .convertFlowListToBO(page.getRecords());
        records.forEach(bo -> {
            if ("00".equals(bo.getFundDirect())) {
                bo.setFundDirectDesc("支出");
            } else if ("01".equals(bo.getFundDirect())) {
                bo.setFundDirectDesc("收入");
            }
        });

        return PageResult.of(records, page.getTotal(),
                dto.getPageNum(), dto.getPageSize());
    }


    // ===================== 贈送金發放 =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantGift(GiftBalanceGrantDTO dto) {
        // 1. 冪等保護
        UserBalanceOrderPO existingOrder = userBalanceManager
                .getByOrderId(dto.getOrderId());
        if (existingOrder != null) {
            if ("2".equals(existingOrder.getStatus())) {
                log.warn("贈送金發放訂單已處理過，orderId={}", dto.getOrderId());
                return;
            }
            throw new BizException(ResultCode.ORDER_EXIST);
        }

        // 2. 確認贈送金帳戶存在（必須先開戶才能發放）
        UserBalancePO giftAccount = userBalanceManager
                .getByUserIdAndType(dto.getUserId(), "1");
        if (giftAccount == null) {
            throw new BizException(ResultCode.GIFT_ACCOUNT_NOT_EXIST);
        }

        // 3. 建立發放訂單
        UserBalanceOrderPO order = new UserBalanceOrderPO();
        order.setOrderId(dto.getOrderId());
        order.setUserId(dto.getUserId());
        order.setAmount(dto.getAmount());
        order.setTradeType("gift");
        order.setStatus("1");
        order.setIsRenew(0);
        userBalanceManager.saveOrder(order);

        // 4. 更新贈送金餘額並寫流水
        updateBalanceAndWriteFlow(giftAccount, dto.getAmount(), "01");

        // 5. 訂單完成
        userBalanceManager.updateOrderStatus(dto.getOrderId(), "2", null);

        log.info("贈送金發放成功，userId={}, orderId={}, amount={}",
                dto.getUserId(), dto.getOrderId(), dto.getAmount());
    }

// ===================== 錢包總覽 =====================

    @Override
    public WalletSummaryBO queryWalletSummary(String userId) {
        List<UserBalancePO> accounts = userBalanceManager.listByUserId(userId);
        if (accounts.isEmpty()) {
            throw new BizException(ResultCode.ACCOUNT_NOT_EXIST);
        }

        UserBalancePO cashAccount = accounts.stream()
                .filter(a -> "0".equals(a.getAccType()))
                .findFirst().orElse(null);
        UserBalancePO giftAccount = accounts.stream()
                .filter(a -> "1".equals(a.getAccType()))
                .findFirst().orElse(null);

        WalletSummaryBO summary = new WalletSummaryBO();
        summary.setUserId(userId);

        summary.setCashBalance(cashAccount != null ? cashAccount.getBalance() : 0L);
        summary.setCashAccNo(cashAccount != null ? cashAccount.getAccNo() : null);

        summary.setGiftBalance(giftAccount != null ? giftAccount.getBalance() : 0L);
        summary.setGiftAccNo(giftAccount != null ? giftAccount.getAccNo() : null);

        summary.setTotalBalance(summary.getCashBalance() + summary.getGiftBalance());

        return summary;
    }
}