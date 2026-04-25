package com.example.wallet;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional  // 每個測試方法結束後自動回滾
public abstract class BaseIntegrationTest {
    // 所有整合測試繼承這個類別
}