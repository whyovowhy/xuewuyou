package com.tianji.promotion.anno;

import com.tianji.promotion.enums.MyLockStrategy;
import com.tianji.promotion.enums.MyLockType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MyLock {
    String name();

    long waitTime() default 1; 

    long leaseTime() default -1;

    TimeUnit unit() default TimeUnit.SECONDS;

    MyLockType type() default MyLockType.RE_ENTRANT_LOCK;//默认可重入锁
    MyLockStrategy strategy() default MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT;//默认失败后重试
}