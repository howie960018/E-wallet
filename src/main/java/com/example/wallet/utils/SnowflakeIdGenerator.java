package com.example.wallet.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SnowflakeIdGenerator {

    // 起始時間戳（2019-01-01），縮短生成的 ID 長度
    private static final long INITIAL_TIME_STAMP = 1546272000000L;

    // 機器節點 ID 占 10 bits → 最多支援 1024 個節點
    private static final long WORKER_ID_BITS = 10L;

    // 序列號占 12 bits → 每毫秒最多生成 4096 個 ID
    private static final long SEQUENCE_BITS = 12L;

    // 序列號最大值：4095
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // workerId 位移量
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    // 時間戳位移量
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    // 從 application.properties 注入 workerId
    public SnowflakeIdGenerator(@Value("${wallet.snowflake.worker-id:1}") long workerId) {
        long maxWorkerId = ~(-1L << WORKER_ID_BITS);
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException(
                    "workerId 必須在 0 ~ " + maxWorkerId + " 之間"
            );
        }
        this.workerId = workerId;
        log.info("SnowflakeIdGenerator 初始化完成，workerId={}", workerId);
    }

    /**
     * 生成下一個全域唯一 ID
     */
    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();

        // 時鐘回撥檢查
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException(
                    "系統時鐘回撥，拒絕生成 ID，回撥時間：" + (lastTimestamp - currentTimestamp) + "ms"
            );
        }

        if (currentTimestamp == lastTimestamp) {
            // 同一毫秒內，序列號 +1
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列號溢出，等待下一毫秒
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列號重置為 0
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - INITIAL_TIME_STAMP) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成流水號：格式為 YYYYMMDD + Snowflake ID
     * 例如：20240101182361409024001
     */
    public String nextFlowNo() {
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return date + nextId();
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}