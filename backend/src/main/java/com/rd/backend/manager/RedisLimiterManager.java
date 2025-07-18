package com.rd.backend.manager;

import com.rd.backend.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import com.rd.backend.common.ErrorCode;

import javax.annotation.Resource;

@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     *
     * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
     */
    public void doRateLimit(String key) {
        // 1. 设置限流规则（只需要设置一次，可复用）
        RRateLimiter rRateLimiter = redissonClient.getRateLimiter(key);
        rRateLimiter.trySetRate(
                RateType.OVERALL,      // 限流类型：OVERALL -> 整体限流；PER_CLIENT -> 按客户端限流
                2,                     // 每个时间窗口内的最大许可量
                1,                     // 时间窗口大小
                RateIntervalUnit.SECONDS // 时间单位
        );


        // 2. 获取许可（每次调用限流操作时）
        boolean acquired = rRateLimiter.tryAcquire(1);
        if (!acquired) {
            // 拒绝或走降级逻辑
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }

    }
}