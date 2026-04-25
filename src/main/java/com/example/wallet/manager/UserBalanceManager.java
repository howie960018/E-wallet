package com.example.wallet.manager;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wallet.domain.po.UserBalanceFlowPO;
import com.example.wallet.domain.po.UserBalanceOrderPO;
import com.example.wallet.domain.po.UserBalancePO;
import com.example.wallet.mapper.UserBalanceFlowMapper;
import com.example.wallet.mapper.UserBalanceMapper;
import com.example.wallet.mapper.UserBalanceOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBalanceManager {

    private final UserBalanceMapper userBalanceMapper;
    private final UserBalanceOrderMapper userBalanceOrderMapper;
    private final UserBalanceFlowMapper userBalanceFlowMapper;

    // ===================== user_balance =====================

    /**
     * 根據 accNo 查詢帳戶（for update 由 Service 層的事務控制）
     */
    public UserBalancePO getByAccNo(String accNo) {
        return userBalanceMapper.selectOne(
                new LambdaQueryWrapper<UserBalancePO>()
                        .eq(UserBalancePO::getAccNo, accNo)
        );
    }

    /**
     * 根據 userId + accType 查詢帳戶
     */
    public UserBalancePO getByUserIdAndType(String userId, String accType) {
        return userBalanceMapper.selectOne(
                new LambdaQueryWrapper<UserBalancePO>()
                        .eq(UserBalancePO::getUserId, userId)
                        .eq(UserBalancePO::getAccType, accType)
        );
    }

    /**
     * 新增帳戶
     */
    public void saveAccount(UserBalancePO po) {
        userBalanceMapper.insert(po);
    }

    /**
     * 更新餘額（樂觀鎖保護，只更新 balance 欄位）
     */
    public int updateBalance(Long id, Long newBalance) {
        return userBalanceMapper.update(null,
                new LambdaUpdateWrapper<UserBalancePO>()
                        .eq(UserBalancePO::getId, id)
                        .set(UserBalancePO::getBalance, newBalance)
        );
    }

    // ===================== user_balance_order =====================

    /**
     * 根據 orderId 查詢訂單
     */
    public UserBalanceOrderPO getByOrderId(String orderId) {
        return userBalanceOrderMapper.selectOne(
                new LambdaQueryWrapper<UserBalanceOrderPO>()
                        .eq(UserBalanceOrderPO::getOrderId, orderId)
        );
    }

    /**
     * 新增訂單
     */
    public void saveOrder(UserBalanceOrderPO po) {
        userBalanceOrderMapper.insert(po);
    }

    /**
     * 更新訂單狀態與支付流水號
     */
    public void updateOrderStatus(String orderId, String status, String tradeNo) {
        userBalanceOrderMapper.update(null,
                new LambdaUpdateWrapper<UserBalanceOrderPO>()
                        .eq(UserBalanceOrderPO::getOrderId, orderId)
                        .set(UserBalanceOrderPO::getStatus, status)
                        .set(UserBalanceOrderPO::getTradeNo, tradeNo)
        );
    }

    // ===================== user_balance_flow =====================

    /**
     * 新增流水紀錄
     */
    public void saveFlow(UserBalanceFlowPO po) {
        userBalanceFlowMapper.insert(po);
    }


    /**
     * 根據 userId 查詢所有帳戶（現金 + 贈送金）
     */
    public List<UserBalancePO> listByUserId(String userId) {
        return userBalanceMapper.selectList(
                new LambdaQueryWrapper<UserBalancePO>()
                        .eq(UserBalancePO::getUserId, userId)
        );
    }

    /**
     * 更新訂單交易類型（用於標記原始訂單已退款）
     */
    public void updateOrderTradeType(String orderId, String tradeType) {
        userBalanceOrderMapper.update(null,
                new LambdaUpdateWrapper<UserBalanceOrderPO>()
                        .eq(UserBalanceOrderPO::getOrderId, orderId)
                        .set(UserBalanceOrderPO::getTradeType, tradeType)
        );
    }

    /**
     * 分頁查詢流水明細
     */
    public IPage<UserBalanceFlowPO> pageFlow(String userId,
                                             String accNo,
                                             String fundDirect,
                                             Integer pageNum,
                                             Integer pageSize) {
        Page<UserBalanceFlowPO> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<UserBalanceFlowPO> wrapper =
                new LambdaQueryWrapper<UserBalanceFlowPO>()
                        .eq(UserBalanceFlowPO::getUserId, userId)
                        .eq(accNo != null, UserBalanceFlowPO::getAccNo, accNo)
                        .eq(fundDirect != null, UserBalanceFlowPO::getFundDirect, fundDirect)
                        .orderByDesc(UserBalanceFlowPO::getCreateTime);

        return userBalanceFlowMapper.selectPage(page, wrapper);
    }



}