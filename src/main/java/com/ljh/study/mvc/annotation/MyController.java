package com.ljh.study.mvc.annotation;


import java.lang.annotation.*;


/**
 * @description: 自定义Controller注解
 * @author: Jh Lee
 * @create: 2019-03-26 15:10
 **/

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {

    String value() default "";
}
