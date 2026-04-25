package com.example.wallet.domain.po;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_balance_order")
public class UserBalanceOrderPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderId;

    private String userId;

    private Long amount;

    private String tradeType;

    private String tradeNo;

    private String status;

    private Integer isRenew;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
