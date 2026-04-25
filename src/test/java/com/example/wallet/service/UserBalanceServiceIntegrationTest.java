package com.example.wallet.service;

import com.example.wallet.BaseIntegrationTest;
import com.example.wallet.common.result.BizException;
import com.example.wallet.common.result.PageResult;
import com.example.wallet.common.result.ResultCode;
import com.example.wallet.domain.bo.AccountBO;
import com.example.wallet.domain.bo.BalanceFlowBO;
import com.example.wallet.domain.bo.WalletSummaryBO;
import com.example.wallet.domain.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserBalanceService 整合測試")
class UserBalanceServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserBalanceService userBalanceService;

    // ===================== 共用方法 =====================

    private AccountBO createCashAccount(String userId) {
        AccountOpenDTO dto = new AccountOpenDTO();
        dto.setUserId(userId);
        dto.setAccType("0");
        return userBalanceService.openAccount(dto);
    }

    private AccountBO createGiftAccount(String userId) {
        AccountOpenDTO dto = new AccountOpenDTO();
        dto.setUserId(userId);
        dto.setAccType("1");
        return userBalanceService.openAccount(dto);
    }

    private void chargeAccount(String userId, String orderId, long amount) {
        BalanceChargeDTO dto = new BalanceChargeDTO();
        dto.setUserId(userId);
        dto.setOrderId(orderId);
        dto.setAmount(amount);
        dto.setTradeNo("PAY_" + orderId);
        userBalanceService.charge(dto);
    }

    private void grantGift(String userId, String orderId, long amount) {
        GiftBalanceGrantDTO dto = new GiftBalanceGrantDTO();
        dto.setUserId(userId);
        dto.setOrderId(orderId);
        dto.setAmount(amount);
        userBalanceService.grantGift(dto);
    }

    private FlowQueryDTO buildFlowQuery(String userId, String fundDirect) {
        FlowQueryDTO dto = new FlowQueryDTO();
        dto.setUserId(userId);
        dto.setFundDirect(fundDirect);
        dto.setPageNum(1);
        dto.setPageSize(20);
        return dto;
    }

    // ===================== 開戶 =====================

    @Nested
    @DisplayName("開戶")
    class OpenAccountTest {

        @Test
        @DisplayName("開戶後餘額應為 0，帳號格式正確")
        void openAccount_balanceIsZero() {
            AccountBO result = createCashAccount("user_it_001");

            assertThat(result.getUserId()).isEqualTo("user_it_001");
            assertThat(result.getBalance()).isEqualTo(0L);
            assertThat(result.getAccNo()).startsWith("ACC");
            assertThat(result.getAccType()).isEqualTo("0");
        }

        @Test
        @DisplayName("同一用戶同類型帳戶不能重複開戶")
        void openAccount_duplicate_shouldThrow() {
            createCashAccount("user_it_002");

            assertThatThrownBy(() -> createCashAccount("user_it_002"))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ACCOUNT_EXIST));
        }

        @Test
        @DisplayName("同一用戶可以同時開現金和贈送金兩個帳戶")
        void openAccount_cashAndGift_bothSuccess() {
            AccountBO cash = createCashAccount("user_it_003");
            AccountBO gift = createGiftAccount("user_it_003");

            assertThat(cash.getAccType()).isEqualTo("0");
            assertThat(gift.getAccType()).isEqualTo("1");
            assertThat(cash.getAccNo()).isNotEqualTo(gift.getAccNo());
        }
    }

    // ===================== 充值 =====================

    @Nested
    @DisplayName("充值")
    class ChargeTest {

        @Test
        @DisplayName("充值後餘額應正確增加，流水應寫入")
        void charge_balanceIncrease_flowCreated() {
            createCashAccount("user_it_010");

            chargeAccount("user_it_010", "ORDER_IT_001", 10000L);

            AccountBO account = userBalanceService.queryAccount("user_it_010", "0");
            assertThat(account.getBalance()).isEqualTo(10000L);

            PageResult<BalanceFlowBO> flows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_010", null));

            assertThat(flows.getTotal()).isEqualTo(1L);
            assertThat(flows.getRecords()).hasSize(1);
            assertThat(flows.getRecords().get(0).getFundDirect()).isEqualTo("01");
            assertThat(flows.getRecords().get(0).getAmount()).isEqualTo(10000L);
            assertThat(flows.getRecords().get(0).getBeginBalance()).isEqualTo(0L);
            assertThat(flows.getRecords().get(0).getEndBalance()).isEqualTo(10000L);
        }

        @Test
        @DisplayName("多次充值餘額應累加")
        void charge_multiple_balanceAccumulate() {
            createCashAccount("user_it_011");

            chargeAccount("user_it_011", "ORDER_IT_002", 5000L);
            chargeAccount("user_it_011", "ORDER_IT_003", 3000L);

            AccountBO account = userBalanceService.queryAccount("user_it_011", "0");
            assertThat(account.getBalance()).isEqualTo(8000L);
        }

        @Test
        @DisplayName("相同 orderId 重複充值應冪等，餘額不重複增加")
        void charge_sameOrderId_idempotent() {
            createCashAccount("user_it_012");

            chargeAccount("user_it_012", "ORDER_IT_004", 5000L);
            chargeAccount("user_it_012", "ORDER_IT_004", 5000L);

            AccountBO account = userBalanceService.queryAccount("user_it_012", "0");
            assertThat(account.getBalance()).isEqualTo(5000L);

            PageResult<BalanceFlowBO> flows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_012", null));

            assertThat(flows.getTotal()).isEqualTo(1L);
        }

        @Test
        @DisplayName("未開戶直接充值應拋出 ACCOUNT_NOT_EXIST")
        void charge_withoutAccount_shouldThrow() {
            BalanceChargeDTO dto = new BalanceChargeDTO();
            dto.setUserId("user_it_no_account");
            dto.setOrderId("ORDER_IT_NO_ACCOUNT");
            dto.setAmount(1000L);
            dto.setTradeNo("PAY_NO_ACCOUNT");

            assertThatThrownBy(() -> userBalanceService.charge(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ACCOUNT_NOT_EXIST));
        }
    }

    // ===================== 扣款 =====================

    @Nested
    @DisplayName("扣款")
    class DeductTest {

        @Test
        @DisplayName("扣款後餘額應正確減少，流水應寫入")
        void deduct_balanceDecrease_flowCreated() {
            createCashAccount("user_it_020");
            chargeAccount("user_it_020", "ORDER_IT_010", 10000L);

            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_it_020");
            dto.setOrderId("DEDUCT_IT_001");
            dto.setAmount(3000L);
            dto.setUseGiftFirst(false);

            userBalanceService.deduct(dto);

            AccountBO account = userBalanceService.queryAccount("user_it_020", "0");
            assertThat(account.getBalance()).isEqualTo(7000L);

            PageResult<BalanceFlowBO> expenseFlows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_020", "00"));

            assertThat(expenseFlows.getTotal()).isEqualTo(1L);
            assertThat(expenseFlows.getRecords()).hasSize(1);
            assertThat(expenseFlows.getRecords().get(0).getAmount()).isEqualTo(3000L);
            assertThat(expenseFlows.getRecords().get(0).getBeginBalance()).isEqualTo(10000L);
            assertThat(expenseFlows.getRecords().get(0).getEndBalance()).isEqualTo(7000L);
        }

        @Test
        @DisplayName("優先扣贈送金 - 贈送金用完再扣現金")
        void deduct_giftFirst_giftExhaustedThenCash() {
            createCashAccount("user_it_021");
            createGiftAccount("user_it_021");

            chargeAccount("user_it_021", "ORDER_IT_011", 10000L);
            grantGift("user_it_021", "GIFT_IT_001", 3000L);

            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_it_021");
            dto.setOrderId("DEDUCT_IT_002");
            dto.setAmount(5000L);
            dto.setUseGiftFirst(true);

            userBalanceService.deduct(dto);

            AccountBO cash = userBalanceService.queryAccount("user_it_021", "0");
            AccountBO gift = userBalanceService.queryAccount("user_it_021", "1");

            assertThat(gift.getBalance()).isEqualTo(0L);
            assertThat(cash.getBalance()).isEqualTo(8000L);

            PageResult<BalanceFlowBO> expenseFlows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_021", "00"));

            assertThat(expenseFlows.getTotal()).isEqualTo(2L);
        }

        @Test
        @DisplayName("餘額不足應拋出例外，餘額不應變動")
        void deduct_insufficientBalance_noDataChange() {
            createCashAccount("user_it_022");
            chargeAccount("user_it_022", "ORDER_IT_012", 1000L);

            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_it_022");
            dto.setOrderId("DEDUCT_IT_003");
            dto.setAmount(9999L);
            dto.setUseGiftFirst(false);

            assertThatThrownBy(() -> userBalanceService.deduct(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.BALANCE_NOT_ENOUGH));

            AccountBO account = userBalanceService.queryAccount("user_it_022", "0");
            assertThat(account.getBalance()).isEqualTo(1000L);

            PageResult<BalanceFlowBO> expenseFlows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_022", "00"));

            assertThat(expenseFlows.getTotal()).isEqualTo(0L);
        }

        @Test
        @DisplayName("未開戶直接扣款應拋出 ACCOUNT_NOT_EXIST")
        void deduct_withoutAccount_shouldThrow() {
            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_it_no_deduct_account");
            dto.setOrderId("DEDUCT_IT_NO_ACCOUNT");
            dto.setAmount(1000L);
            dto.setUseGiftFirst(false);

            assertThatThrownBy(() -> userBalanceService.deduct(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ACCOUNT_NOT_EXIST));
        }

        @Test
        @DisplayName("相同 orderId 重複扣款應冪等，餘額不重複減少")
        void deduct_sameOrderId_idempotent() {
            createCashAccount("user_it_023");
            chargeAccount("user_it_023", "ORDER_IT_013", 10000L);

            BalanceDeductDTO dto = new BalanceDeductDTO();
            dto.setUserId("user_it_023");
            dto.setOrderId("DEDUCT_IT_004");
            dto.setAmount(3000L);
            dto.setUseGiftFirst(false);

            userBalanceService.deduct(dto);
            userBalanceService.deduct(dto);

            AccountBO account = userBalanceService.queryAccount("user_it_023", "0");
            assertThat(account.getBalance()).isEqualTo(7000L);

            PageResult<BalanceFlowBO> expenseFlows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_023", "00"));

            assertThat(expenseFlows.getTotal()).isEqualTo(1L);
        }
    }

    // ===================== 退款 =====================

    @Nested
    @DisplayName("退款")
    class RefundTest {

        @Test
        @DisplayName("退款後現金餘額應正確增加")
        void refund_cashBalanceIncrease() {
            createCashAccount("user_it_030");
            chargeAccount("user_it_030", "ORDER_IT_020", 10000L);

            BalanceDeductDTO deductDTO = new BalanceDeductDTO();
            deductDTO.setUserId("user_it_030");
            deductDTO.setOrderId("DEDUCT_IT_010");
            deductDTO.setAmount(5000L);
            deductDTO.setUseGiftFirst(false);
            userBalanceService.deduct(deductDTO);

            BalanceRefundDTO refundDTO = new BalanceRefundDTO();
            refundDTO.setUserId("user_it_030");
            refundDTO.setOriginalOrderId("DEDUCT_IT_010");
            refundDTO.setRefundOrderId("REFUND_IT_001");
            refundDTO.setAmount(5000L);
            userBalanceService.refund(refundDTO);

            AccountBO account = userBalanceService.queryAccount("user_it_030", "0");
            assertThat(account.getBalance()).isEqualTo(10000L);

            PageResult<BalanceFlowBO> incomeFlows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_030", "01"));

            assertThat(incomeFlows.getTotal()).isEqualTo(2L);
        }

        @Test
        @DisplayName("同一原始扣款訂單不能退款兩次")
        void refund_twice_shouldThrow() {
            createCashAccount("user_it_031");
            chargeAccount("user_it_031", "ORDER_IT_021", 10000L);

            BalanceDeductDTO deductDTO = new BalanceDeductDTO();
            deductDTO.setUserId("user_it_031");
            deductDTO.setOrderId("DEDUCT_IT_011");
            deductDTO.setAmount(5000L);
            deductDTO.setUseGiftFirst(false);
            userBalanceService.deduct(deductDTO);

            BalanceRefundDTO refundDTO = new BalanceRefundDTO();
            refundDTO.setUserId("user_it_031");
            refundDTO.setOriginalOrderId("DEDUCT_IT_011");
            refundDTO.setRefundOrderId("REFUND_IT_002");
            refundDTO.setAmount(5000L);
            userBalanceService.refund(refundDTO);

            BalanceRefundDTO refundDTO2 = new BalanceRefundDTO();
            refundDTO2.setUserId("user_it_031");
            refundDTO2.setOriginalOrderId("DEDUCT_IT_011");
            refundDTO2.setRefundOrderId("REFUND_IT_003");
            refundDTO2.setAmount(5000L);

            assertThatThrownBy(() -> userBalanceService.refund(refundDTO2))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ORDER_ALREADY_REFUNDED));
        }

        @Test
        @DisplayName("退款金額超過原始扣款金額應拋出 PARAM_ERROR")
        void refund_amountExceed_shouldThrow() {
            createCashAccount("user_it_032");
            chargeAccount("user_it_032", "ORDER_IT_022", 10000L);

            BalanceDeductDTO deductDTO = new BalanceDeductDTO();
            deductDTO.setUserId("user_it_032");
            deductDTO.setOrderId("DEDUCT_IT_012");
            deductDTO.setAmount(3000L);
            deductDTO.setUseGiftFirst(false);
            userBalanceService.deduct(deductDTO);

            BalanceRefundDTO refundDTO = new BalanceRefundDTO();
            refundDTO.setUserId("user_it_032");
            refundDTO.setOriginalOrderId("DEDUCT_IT_012");
            refundDTO.setRefundOrderId("REFUND_IT_004");
            refundDTO.setAmount(9999L);

            assertThatThrownBy(() -> userBalanceService.refund(refundDTO))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.PARAM_ERROR));
        }

        @Test
        @DisplayName("原始訂單不存在時退款應拋出 ORDER_NOT_EXIST")
        void refund_originalOrderNotExist_shouldThrow() {
            createCashAccount("user_it_033");

            BalanceRefundDTO refundDTO = new BalanceRefundDTO();
            refundDTO.setUserId("user_it_033");
            refundDTO.setOriginalOrderId("DEDUCT_NOT_EXIST");
            refundDTO.setRefundOrderId("REFUND_IT_005");
            refundDTO.setAmount(1000L);

            assertThatThrownBy(() -> userBalanceService.refund(refundDTO))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ORDER_NOT_EXIST));
        }
    }

    // ===================== 贈送金 =====================

    @Nested
    @DisplayName("贈送金")
    class GiftTest {

        @Test
        @DisplayName("發放贈送金後贈送金餘額應增加")
        void grantGift_balanceIncrease() {
            createGiftAccount("user_it_060");

            grantGift("user_it_060", "GIFT_IT_060", 3000L);

            AccountBO gift = userBalanceService.queryAccount("user_it_060", "1");
            assertThat(gift.getBalance()).isEqualTo(3000L);

            PageResult<BalanceFlowBO> incomeFlows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_060", "01"));

            assertThat(incomeFlows.getTotal()).isEqualTo(1L);
            assertThat(incomeFlows.getRecords().get(0).getAmount()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("未開贈送金帳戶時發放贈送金應拋出 GIFT_ACCOUNT_NOT_EXIST")
        void grantGift_withoutGiftAccount_shouldThrow() {
            createCashAccount("user_it_061");

            GiftBalanceGrantDTO dto = new GiftBalanceGrantDTO();
            dto.setUserId("user_it_061");
            dto.setOrderId("GIFT_IT_061");
            dto.setAmount(1000L);

            assertThatThrownBy(() -> userBalanceService.grantGift(dto))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.GIFT_ACCOUNT_NOT_EXIST));
        }

        @Test
        @DisplayName("相同 orderId 重複發放贈送金應冪等")
        void grantGift_sameOrderId_idempotent() {
            createGiftAccount("user_it_062");

            grantGift("user_it_062", "GIFT_IT_062", 3000L);
            grantGift("user_it_062", "GIFT_IT_062", 3000L);

            AccountBO gift = userBalanceService.queryAccount("user_it_062", "1");
            assertThat(gift.getBalance()).isEqualTo(3000L);

            PageResult<BalanceFlowBO> incomeFlows =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_062", "01"));

            assertThat(incomeFlows.getTotal()).isEqualTo(1L);
        }
    }

    // ===================== 查詢流水 =====================

    @Nested
    @DisplayName("查詢流水")
    class FlowQueryTest {

        @Test
        @DisplayName("流水筆數與方向應與實際操作一致")
        void queryFlow_countAndDirection() {
            createCashAccount("user_it_040");

            chargeAccount("user_it_040", "ORDER_IT_030", 10000L);
            chargeAccount("user_it_040", "ORDER_IT_031", 5000L);

            BalanceDeductDTO deductDTO = new BalanceDeductDTO();
            deductDTO.setUserId("user_it_040");
            deductDTO.setOrderId("DEDUCT_IT_020");
            deductDTO.setAmount(3000L);
            deductDTO.setUseGiftFirst(false);

            userBalanceService.deduct(deductDTO);

            PageResult<BalanceFlowBO> all =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_040", null));

            PageResult<BalanceFlowBO> income =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_040", "01"));

            PageResult<BalanceFlowBO> expense =
                    userBalanceService.queryFlow(buildFlowQuery("user_it_040", "00"));

            assertThat(all.getTotal()).isEqualTo(3L);
            assertThat(income.getTotal()).isEqualTo(2L);
            assertThat(expense.getTotal()).isEqualTo(1L);
        }

        @Test
        @DisplayName("分頁應正確切割資料")
        void queryFlow_pagination() {
            createCashAccount("user_it_041");

            for (int i = 1; i <= 5; i++) {
                chargeAccount("user_it_041", "ORDER_IT_04" + i, 1000L * i);
            }

            FlowQueryDTO dto = buildFlowQuery("user_it_041", null);
            dto.setPageNum(1);
            dto.setPageSize(2);

            PageResult<BalanceFlowBO> page1 = userBalanceService.queryFlow(dto);

            assertThat(page1.getTotal()).isEqualTo(5L);
            assertThat(page1.getRecords()).hasSize(2);
            assertThat(page1.getTotalPages()).isEqualTo(3);
        }
    }

    // ===================== 錢包總覽 =====================

    @Nested
    @DisplayName("錢包總覽")
    class WalletSummaryTest {

        @Test
        @DisplayName("總覽應正確加總現金與贈送金")
        void queryWalletSummary_totalBalance() {
            createCashAccount("user_it_050");
            createGiftAccount("user_it_050");

            chargeAccount("user_it_050", "ORDER_IT_050", 10000L);
            grantGift("user_it_050", "GIFT_IT_050", 3000L);

            WalletSummaryBO summary =
                    userBalanceService.queryWalletSummary("user_it_050");

            assertThat(summary.getUserId()).isEqualTo("user_it_050");
            assertThat(summary.getCashBalance()).isEqualTo(10000L);
            assertThat(summary.getGiftBalance()).isEqualTo(3000L);
            assertThat(summary.getTotalBalance()).isEqualTo(13000L);
            assertThat(summary.getCashAccNo()).isNotBlank();
            assertThat(summary.getGiftAccNo()).isNotBlank();
        }

        @Test
        @DisplayName("只有現金帳戶時贈送金顯示為 0")
        void queryWalletSummary_noGiftAccount() {
            createCashAccount("user_it_051");
            chargeAccount("user_it_051", "ORDER_IT_051", 5000L);

            WalletSummaryBO summary =
                    userBalanceService.queryWalletSummary("user_it_051");

            assertThat(summary.getCashBalance()).isEqualTo(5000L);
            assertThat(summary.getGiftBalance()).isEqualTo(0L);
            assertThat(summary.getTotalBalance()).isEqualTo(5000L);
            assertThat(summary.getCashAccNo()).isNotBlank();
            assertThat(summary.getGiftAccNo()).isNull();
        }

        @Test
        @DisplayName("沒有任何帳戶時查詢總覽應拋出 ACCOUNT_NOT_EXIST")
        void queryWalletSummary_noAccount_shouldThrow() {
            assertThatThrownBy(() ->
                    userBalanceService.queryWalletSummary("user_it_no_summary_account"))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getResultCode())
                            .isEqualTo(ResultCode.ACCOUNT_NOT_EXIST));
        }
    }
}