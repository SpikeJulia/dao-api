package com.spike.dao.api.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author tangxuan
 * @date 2020/12/17 21:27
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ApiMapping {
    /**
     * 注解的需要使用dao-api的方法
     *
     * @return
     */
    String value();
}
