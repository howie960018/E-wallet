package com.example.wallet.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_balance")
public class UserBalancePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String accNo;

    private String accType;

    private Long balance;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}