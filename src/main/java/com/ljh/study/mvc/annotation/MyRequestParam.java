package com.ljh.study.mvc.annotation;

import java.lang.annotation.*;

/**
 * @description: 自定义@RequestParam
 * @author: Jh Lee
 * @create: 2019-03-26 21:12
 **/
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {

    String value() default "";
}
