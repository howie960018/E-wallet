package com.example.wallet.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_balance_flow")
public class UserBalanceFlowPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String flowNo;

    private String accNo;

    private Long amount;

    private Long beginBalance;

    private Long endBalance;

    private String fundDirect;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}