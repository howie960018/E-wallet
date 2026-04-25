package com.example.wallet;


import com.example.wallet.utils.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;

@SpringBootTest
@ActiveProfiles("test")
public class SnowflakeIdGeneratorTest {

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Test
    public void testGenerateId() {
        // 生成 10 個 ID，印出來看格式
        for (int i = 0; i < 10; i++) {
            System.out.println("ID：" + snowflakeIdGenerator.nextId());
            System.out.println("流水號：" + snowflakeIdGenerator.nextFlowNo());
        }
    }

    @Test
    public void testUniqueness() {
        // 生成 10000 個 ID，確認沒有重複
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            ids.add(snowflakeIdGenerator.nextId());
        }
        System.out.println("生成數量：10000，不重複數量：" + ids.size());
        assert ids.size() == 10000 : "發現重複 ID！";
    }
}