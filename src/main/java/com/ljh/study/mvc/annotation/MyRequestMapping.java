package com.ljh.study.mvc.annotation;

import java.lang.annotation.*;

/**
 * @description: 自定义@RequestMapping
 * @author: Jh Lee
 * @create: 2019-03-26 20:02
 **/
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {

    String value() default "";
}
