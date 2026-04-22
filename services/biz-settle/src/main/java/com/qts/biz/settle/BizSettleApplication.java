package com.qts.biz.settle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BIZ-SETTLE 清结算服务
 * 
 * 职责：
 * - 日终清算（T+1 00:30执行）
 * - 实时对账（每分钟）
 * - 账户余额核对
 * - 持仓对账
 * - 佣金/费用计算
 */
@SpringBootApplication
@EnableScheduling
public class BizSettleApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizSettleApplication.class, args);
    }
}
