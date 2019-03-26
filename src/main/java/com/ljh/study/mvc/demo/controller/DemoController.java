package com.ljh.study.mvc.demo.controller;

import com.ljh.study.mvc.annotation.MyController;
import com.ljh.study.mvc.annotation.MyRequestMapping;
import com.ljh.study.mvc.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @description: 使用自定义注解的controller
 * @author: Jh Lee
 * @create: 2019-03-26 16:52
 **/
@MyController
@MyRequestMapping
public class DemoController {


    @MyRequestMapping("/query")
    public void queryName(HttpServletRequest req, HttpServletResponse resp,
                          @MyRequestParam("name") String name){
        String result = "My name is " + name;
        try {
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html;charset=utf-8");
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
