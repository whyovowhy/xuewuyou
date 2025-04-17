package com.tianji.promotion.aspect;

import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.promotion.anno.MyLock;
import com.tianji.promotion.enums.MyLockFactory;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
@Aspect
@RequiredArgsConstructor
public class MyLockAspect implements Ordered {

    private final MyLockFactory myLockFactory;

    @Around("@annotation(myLock)")
    public Object tryLock(ProceedingJoinPoint pjp, MyLock myLock) throws Throwable {
        // 1.创建锁对象\
//         RLock lock = redissonClient.getLock(myLock.name());  // 只能获取可重入锁
        RLock lock = myLockFactory.getLock(myLock.type(),myLock.name());
        // 2.尝试获取锁
        // boolean isLock = lock.tryLock(myLock.waitTime(), myLock.leaseTime(), myLock.unit()); // 无重试，指定重试
        boolean isLock = myLock.strategy().tryLock(lock,myLock);//采用策略模式，让用户选择想要的策略
        // 3.判断是否成功
        if(!isLock) {
            // 3.1.失败，快速结束
            return null;
        }
        try {
            // 3.2.成功，执行业务
            return pjp.proceed();
        } finally {
            // 4.释放锁
            lock.unlock();
        }
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
}