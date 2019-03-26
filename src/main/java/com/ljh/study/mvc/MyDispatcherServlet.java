package com.ljh.study.mvc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @description: 自定义dispatcherServlet
 * @author: Jh Lee
 * @create: 2019-03-26 14:47
 **/
public class MyDispatcherServlet extends HttpServlet {

    //存储application.properties的配置内容
    private Properties contextConfig = new Properties();

    //存储所有扫描到的类
    private List<String> classNames = new ArrayList<String>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件,先获得在xml中配置的key为contextConfigLocation的value值，进行配置文件加载
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2、扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3、初始化IOC，实例化所有相关的类
        
        //4、完成DI操作，依赖注入
        
        //5、初始化HandlerMapping(把我们自定义的注解)
        System.out.println("MyDispatcherServlet is init..");
    }

    private void doScanner(String scanPackage) {
        //获取我们需要扫面的包路径
        URL packageUrl = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        //获取这个路径下的全部文件
        File file = new File(packageUrl.getFile());
        //遍历我们需要扫描到的包下面全部的文件包括文件夹
        for(File f : file.listFiles()){
            //如果这个文件对象是一个目录不是文件的话
            if(f.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            }else{
                //如果不是我们项目打成的class文件时退出循环
                if(f.getName().endsWith(".class"))continue;
                //获取我们的文件类名
                String className = (scanPackage + "." + f.getName()).replace(".class", "");
                //放入我们存储文件名的list集合中，放入内存中存储
                classNames.add(className);
            }
        }
    }

    //加载配置文件,
    private void doLoadConfig(String contextConfigLocation) {
        //通过
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != resourceAsStream){
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
