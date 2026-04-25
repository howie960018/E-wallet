package com.example.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wallet.domain.po.UserBalancePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBalanceMapper extends BaseMapper<UserBalancePO> {
}