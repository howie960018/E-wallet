package com.example.wallet.domain.bo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AccountBO implements Serializable {

    private Long id;

    private String userId;

    private String accNo;

    private String accType;

    private Long balance;

    private LocalDateTime createTime;
}