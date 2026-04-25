package com.example.wallet.service;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import com.example.wallet.common.result.BizException;
import com.example.wallet.common.result.ResultCode;
import com.example.wallet.domain.bo.AccountBO;
import com.example.wallet.domain.dto.AccountOpenDTO;
import com.example.wallet.domain.dto.BalanceChargeDTO;
import com.example.wallet.domain.dto.BalanceDeductDTO;
import com.example.wallet.domain.dto.BalanceRefundDTO;
import com.example.wallet.domain.dto.GiftBalanceGrantDTO;
import com.example.wallet.domain.po.UserBalanceOrderPO;
import com.example.wallet.domain.po.UserBalancePO;
import com.example.wallet.manager.UserBalanceManager;
import com.example.wallet.service.impl.UserBalanceServiceImpl;
import com.example.wallet.utils.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBalanceService 單元測試")
class UserBalanceServiceTest {

    @Mock
    private UserBalanceManager userBalanceManager;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private UserBalanceServiceImpl userBalanceService;

    @BeforeEach
    void setup() {
        // By default, simulate successful optimistic-lock update (1 row affected)
        lenient().when(userBalanceManager.updateBalance(any(UserBalancePO.class), any()))
                .thenReturn(1);
    }

    // ===================== 共用測試資料 =====================

    private UserBalancePO mockCashAccount() {
        UserBalancePO po = new UserBalancePO();
        po.setId(1L);
        po.setUserId("user_001");
        po.setAccNo("ACC00000001");
        po.setAccType("0");
        po.setBalance(10000L);
        return po;
    }

    private UserBalancePO mockGiftAccount() {
        UserBalancePO po = new UserBalancePO();
        po.setId(2L);
        po.setUserId("user_001");
        po.setAccNo("ACC00000002");
        po.setAccType("1");
        po.setBalance(5000L);
        return po;
    }

    private UserBalanceOrderPO mockSuccessOrder(String orderId, String tradeType, Long amount) {
        UserBalanceOrderPO order = new UserBalanceOrderPO();
        order.setOrderId(orderId);
        order.setUserId("user_001");
        order.setAmount(amount);
        order.setTradeType(tradeType);
        order.setStatus("2");
        return order;
    }

    // ===================== 開戶測試 =====================

    @Nested
    @DisplayName("開戶")
    class OpenAccountTest {

        @Test
        @DisplayName("正常開戶 - 應成功建立帳戶")
        void openAccount_success() {
            AccountOpenDTO dto = new AccountOpenDTO();
            dto.setUserId("user_001");
            dto.setAccType("0");

            when(userBalanceManager.getByUserIdAndType("user_001", "0"))
                    .thenReturn(null);

            AccountBO result = userBalanceService.openAccount(dto);

            assertThat(result.getUserId()).isEqualTo("user_001");
            assertThat(result.getAccType()).isEqualTo("0");
            assertThat(result.getBalance()).isEqualTo(0L);
            verify(userBalanceManager, times(1)).saveAccount(any());
        }

        @Test
        @DisplayName("重複開戶 - 應拋出 ACCOUNT_EXIST 例外")
        void openAccount_alreadyExist() {
            AccountOpenDTO dto = new AccountOpenDTO();
            dto.setUserId("user_001");
            dto.setAccType("0");

            when(userBalanceManager.getByUserIdAndType("user_001", "0"))
                    .thenReturn(mockCashAccount());

            assertThatThrownBy(() -> userBalanceService.openAccount(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(
                            ((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ACCOUNT_EXIST));

            verify(userBalanceManager, never()).saveAccount(any());
        }
    }

    // ===================== 充值測試 =====================

    @Nested
    @DisplayName("充值")
    class ChargeTest {

        @Test
        @DisplayName("正常充值 - 餘額應正確增加")
        void charge_success() {
            BalanceChargeDTO dto = new BalanceChargeDTO();
            dto.setUserId("user_001");
            dto.setOrderId("ORDER_001");
            dto.setAmount(5000L);
            dto.setTradeNo("PAY_888");

            when(userBalanceManager.getByOrderId("ORDER_001")).thenReturn(null);
            when(userBalanceManager.getByUserIdAndType("user_001", "0"))
                    .thenReturn(mockCashAccount());
            when(snowflakeIdGenerator.nextFlowNo()).thenReturn("20240101001");

            userBalanceService.charge(dto);

            verify(userBalanceManager).updateBalance(argThat(po -> po != null && po.getId() != null && po.getId().longValue() == 1L), eq(15000L));
            verify(userBalanceManager).saveFlow(any());
            verify(userBalanceManager).updateOrderStatus("ORDER_001", "2", "PAY_888");
        }

        @Test
        @DisplayName("重複充值（冪等）- 應直接返回不重複處理")
        void charge_idempotent() {
            BalanceChargeDTO dto = new BalanceChargeDTO();
            dto.setUserId("user_001");
            dto.setOrderId("ORDER_001");
            dto.setAmount(5000L);

            when(userBalanceManager.getByOrderId("ORDER_001"))
                    .thenReturn(mockSuccessOrder("ORDER_001", "charge", 5000L));

            userBalanceService.charge(dto);

            verify(userBalanceManager, never()).updateBalance(any(), any());
            verify(userBalanceManager, never()).saveFlow(any());
        }

        @Test
        @DisplayName("帳戶不存在 - 應拋出 ACCOUNT_NOT_EXIST 例外")
        void charge_accountNotExist() {
            BalanceChargeDTO dto = new BalanceChargeDTO();
            dto.setUserId("user_001");
            dto.setOrderId("ORDER_001");
            dto.setAmount(5000L);

            when(userBalanceManager.getByOrderId("ORDER_001")).thenReturn(null);
            when(userBalanceManager.getByUserIdAndType("user_001", "0")).thenReturn(null);

            assertThatThrownBy(() -> userBalanceService.charge(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(
                            ((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ACCOUNT_NOT_EXIST));
        }


        @Test
        @DisplayName("樂觀鎖衝突 - 應拋出 OPTIMISTIC_LOCK_CONFLICT 例外")
        void charge_optimisticLockConflict() {
            BalanceChargeDTO dto = new BalanceChargeDTO();
            dto.setUserId("user_001");
            dto.setOrderId("ORDER_001");
            dto.setAmount(5000L);
            dto.setTradeNo("PAY_888");

            when(userBalanceManager.getByOrderId("ORDER_001")).thenReturn(null);
            when(userBalanceManager.getByUserIdAndType("user_001", "0"))
                    .thenReturn(mockCashAccount());

            when(userBalanceManager.updateBalance(
                    argThat(po -> po != null && po.getId() != null && po.getId().longValue() == 1L),
                    eq(15000L)
            )).thenReturn(0);

            assertThatThrownBy(() -> userBalanceService.charge(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(
                            ((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.OPTIMISTIC_LOCK_CONFLICT));

            verify(userBalanceManager, never()).updateOrderStatus(anyString(), anyString(), any());
            verify(userBalanceManager, never()).saveFlow(any());
        }
    }

    // ===================== 扣款測試 =====================

    @Nested
    @DisplayName("扣款")
    class DeductTest {

        @Test
        @DisplayName("正常扣款（現金）- 餘額應正確減少")
        void deduct_cashOnly_success() {
            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_001");
            dto.setOrderId("DEDUCT_001");
            dto.setAmount(3000L);
            dto.setUseGiftFirst(false);

            when(userBalanceManager.getByOrderId("DEDUCT_001")).thenReturn(null);
            when(userBalanceManager.listByUserId("user_001"))
                    .thenReturn(List.of(mockCashAccount()));
            when(snowflakeIdGenerator.nextFlowNo()).thenReturn("20240101002");

            userBalanceService.deduct(dto);

            verify(userBalanceManager).updateBalance(argThat(po -> po != null && po.getId() != null && po.getId().longValue() == 1L), eq(7000L));
            verify(userBalanceManager).updateOrderStatus("DEDUCT_001", "2", null);
        }

        @Test
        @DisplayName("優先扣贈送金 - 贈送金先扣，不足再扣現金")
        void deduct_giftFirst_success() {
            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_001");
            dto.setOrderId("DEDUCT_002");
            dto.setAmount(8000L);
            dto.setUseGiftFirst(true);

            when(userBalanceManager.getByOrderId("DEDUCT_002")).thenReturn(null);
            when(userBalanceManager.listByUserId("user_001"))
                    .thenReturn(Arrays.asList(mockCashAccount(), mockGiftAccount()));
            when(snowflakeIdGenerator.nextFlowNo()).thenReturn("20240101003");

            userBalanceService.deduct(dto);

            // 贈送金 5000 全部扣完，現金再扣 3000
            verify(userBalanceManager).updateBalance(argThat(po -> po != null && po.getId() != null && po.getId().longValue() == 2L), eq(0L));
            verify(userBalanceManager).updateBalance(argThat(po -> po != null && po.getId() != null && po.getId().longValue() == 1L), eq(7000L));
            verify(userBalanceManager, times(2)).saveFlow(any());
        }

        @Test
        @DisplayName("餘額不足 - 應拋出 BALANCE_NOT_ENOUGH 例外")
        void deduct_balanceNotEnough() {
            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_001");
            dto.setOrderId("DEDUCT_003");
            dto.setAmount(99999L);
            dto.setUseGiftFirst(false);

            when(userBalanceManager.getByOrderId("DEDUCT_003")).thenReturn(null);
            when(userBalanceManager.listByUserId("user_001"))
                    .thenReturn(List.of(mockCashAccount()));

            assertThatThrownBy(() -> userBalanceService.deduct(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(
                            ((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.BALANCE_NOT_ENOUGH));

            verify(userBalanceManager, never()).updateBalance(any(), any());
        }

        @Test
        @DisplayName("無帳戶 - 應拋出 ACCOUNT_NOT_EXIST 例外")
        void deduct_noAccount() {
            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_001");
            dto.setOrderId("DEDUCT_004");
            dto.setAmount(1000L);

            when(userBalanceManager.getByOrderId("DEDUCT_004")).thenReturn(null);
            when(userBalanceManager.listByUserId("user_001"))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> userBalanceService.deduct(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(
                            ((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ACCOUNT_NOT_EXIST));
        }
    }

    // ===================== 退款測試 =====================

    @Nested
    @DisplayName("退款")
    class RefundTest {

        @Test
        @DisplayName("正常退款 - 金額應退回現金帳戶")
        void refund_success() {
            BalanceRefundDTO dto = new BalanceRefundDTO();
            dto.setUserId("user_001");
            dto.setOriginalOrderId("DEDUCT_001");
            dto.setRefundOrderId("REFUND_001");
            dto.setAmount(3000L);

            when(userBalanceManager.getByOrderId("REFUND_001")).thenReturn(null);
            when(userBalanceManager.getByOrderId("DEDUCT_001"))
                    .thenReturn(mockSuccessOrder("DEDUCT_001", "deduct", 3000L));
            when(userBalanceManager.getByUserIdAndType("user_001", "0"))
                    .thenReturn(mockCashAccount());
            when(snowflakeIdGenerator.nextFlowNo()).thenReturn("20240101004");

            userBalanceService.refund(dto);

            verify(userBalanceManager).updateBalance(argThat(po -> po != null && po.getId() != null && po.getId().longValue() == 1L), eq(13000L));
            verify(userBalanceManager).updateOrderStatus("REFUND_001", "2", null);
            verify(userBalanceManager).updateOrderTradeType("DEDUCT_001", "refund");
        }

        @Test
        @DisplayName("退款金額超過原始訂單 - 應拋出 PARAM_ERROR 例外")
        void refund_amountExceed() {
            BalanceRefundDTO dto = new BalanceRefundDTO();
            dto.setUserId("user_001");
            dto.setOriginalOrderId("DEDUCT_001");
            dto.setRefundOrderId("REFUND_001");
            dto.setAmount(9999L);

            when(userBalanceManager.getByOrderId("REFUND_001")).thenReturn(null);
            when(userBalanceManager.getByOrderId("DEDUCT_001"))
                    .thenReturn(mockSuccessOrder("DEDUCT_001", "deduct", 3000L));

            assertThatThrownBy(() -> userBalanceService.refund(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(
                            ((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.PARAM_ERROR));
        }

        @Test
        @DisplayName("重複退款 - 應拋出 ORDER_ALREADY_REFUNDED 例外")
        void refund_alreadyRefunded() {
            BalanceRefundDTO dto = new BalanceRefundDTO();
            dto.setUserId("user_001");
            dto.setOriginalOrderId("DEDUCT_001");
            dto.setRefundOrderId("REFUND_001");
            dto.setAmount(3000L);

            when(userBalanceManager.getByOrderId("REFUND_001")).thenReturn(null);
            when(userBalanceManager.getByOrderId("DEDUCT_001"))
                    .thenReturn(mockSuccessOrder("DEDUCT_001", "refund", 3000L));

            assertThatThrownBy(() -> userBalanceService.refund(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(
                            ((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ORDER_ALREADY_REFUNDED));
        }
    }

    // ===================== 贈送金測試 =====================

    @Nested
    @DisplayName("贈送金")
    class GiftTest {

        @Test
        @DisplayName("正常發放贈送金 - 餘額應正確增加")
        void grantGift_success() {
            GiftBalanceGrantDTO dto = new GiftBalanceGrantDTO();
            dto.setUserId("user_001");
            dto.setOrderId("GIFT_001");
            dto.setAmount(5000L);

            when(userBalanceManager.getByOrderId("GIFT_001")).thenReturn(null);
            when(userBalanceManager.getByUserIdAndType("user_001", "1"))
                    .thenReturn(mockGiftAccount());
            when(snowflakeIdGenerator.nextFlowNo()).thenReturn("20240101005");

            userBalanceService.grantGift(dto);

            verify(userBalanceManager).updateBalance(argThat(po -> po != null && po.getId() != null && po.getId().longValue() == 2L), eq(10000L));
            verify(userBalanceManager).updateOrderStatus("GIFT_001", "2", null);
        }

        @Test
        @DisplayName("贈送金帳戶不存在 - 應拋出 GIFT_ACCOUNT_NOT_EXIST 例外")
        void grantGift_accountNotExist() {
            GiftBalanceGrantDTO dto = new GiftBalanceGrantDTO();
            dto.setUserId("user_001");
            dto.setOrderId("GIFT_001");
            dto.setAmount(5000L);

            when(userBalanceManager.getByOrderId("GIFT_001")).thenReturn(null);
            when(userBalanceManager.getByUserIdAndType("user_001", "1")).thenReturn(null);

            assertThatThrownBy(() -> userBalanceService.grantGift(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(
                            ((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.GIFT_ACCOUNT_NOT_EXIST));
        }
    }
}
