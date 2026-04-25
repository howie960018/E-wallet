CREATE TABLE IF NOT EXISTS user_balance (
                                            id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
                                            user_id     VARCHAR(60)  NOT NULL,
    acc_no      VARCHAR(60)  NOT NULL,
    acc_type    VARCHAR(2)   NOT NULL,
    balance     BIGINT       NOT NULL DEFAULT 0,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS user_balance_order (
                                                  id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
                                                  order_id    VARCHAR(50)  NOT NULL,
    user_id     VARCHAR(60)  NOT NULL,
    amount      BIGINT       NOT NULL,
    trade_type  VARCHAR(20)  NOT NULL,
    trade_no    VARCHAR(32),
    status      VARCHAR(2)   NOT NULL DEFAULT '0',
    is_renew    INT          NOT NULL DEFAULT 0,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS user_balance_flow (
                                                 id            BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
                                                 user_id       VARCHAR(60)  NOT NULL,
    flow_no       VARCHAR(64)  NOT NULL,
    acc_no        VARCHAR(60)  NOT NULL,
    amount        BIGINT       NOT NULL,
    begin_balance BIGINT       NOT NULL,
    end_balance   BIGINT       NOT NULL,
    fund_direct   VARCHAR(2)   NOT NULL,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    );