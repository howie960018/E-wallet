package com.example.wallet.domain.bo;

import com.example.wallet.domain.po.UserBalanceFlowPO;
import com.example.wallet.domain.po.UserBalancePO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UserBalanceConvert {

    UserBalanceConvert INSTANCE = Mappers.getMapper(UserBalanceConvert.class);

    // PO → BO（從資料庫查出來轉成業務物件）
    AccountBO convertToBO(UserBalancePO po);

    // BO → PO（業務物件轉成資料庫物件）
    UserBalancePO convertToPO(AccountBO bo);

    // ↓ 補上這兩個方法
    BalanceFlowBO convertFlowToBO(UserBalanceFlowPO po);

    List<BalanceFlowBO> convertFlowListToBO(List<UserBalanceFlowPO> poList);
}