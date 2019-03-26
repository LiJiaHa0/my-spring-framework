package com.ljh.study.mvc;

import com.ljh.study.mvc.annotation.MyController;
import com.ljh.study.mvc.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

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

    //存储所有实例化后的bean
    private Map<String,Object> ioc = new HashMap<String,Object>();

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
        doInstance();
        //4、完成DI操作，依赖注入
        
        //5、初始化HandlerMapping(把我们自定义的注解)
        System.out.println("MyDispatcherServlet is init..");
    }

    private void doInstance() {
        if(classNames.isEmpty()) return;
        //遍历类名集合
        for (String className : classNames){
            try {
                //通过类名反射获取类对象
                Class<?> clazz = Class.forName(className);
                //判断是否存在我们自己定义的注解
                if(clazz.isAnnotationPresent(MyController.class)){
                    //通过反射实例化后的对象
                    Object instance = clazz.newInstance();
                    //通过指定把类名第一个字母转成小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,instance);
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    //默认小写自定义类名
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    //实例化被MyService修饰的类
                    Object instance = clazz.newInstance();
                    //判断是否存在自定义的service类名
                    MyService service = clazz.getAnnotation(MyService.class);
                    //如果用户自己自定义了MyService的值，则用用户自定义的命用作key
                    if(!"".equals(service.value())){
                        beanName = service.value();
                        ioc.put(beanName,instance);
                    }

                    //如果自己没设，就按接口类型创建一个实例
                    for (Class<?> i : clazz.getInterfaces()) {
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The “" + i.getName() + "” is exists!!");
                        }
                        //把接口的类型直接当成key了
                        ioc.put(i.getName(),instance);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    //扫描配置文件中的扫描的包路径下的所有类存入list中存入内存中
    private void doScanner(String scanPackage) {
        //获取我们需要扫描的包路径，（com.ljh.study -> /com/ljh/study）
        URL packageUrl = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        //获取这个路径下的全部文件
        File file = new File(packageUrl.getFile());
        //遍历我们需要扫描到的包下面全部的文件包括文件夹
        for(File f : file.listFiles()){
            //如果这个文件对象是一个目录不是文件的话
            if(f.isDirectory()){
                doScanner(scanPackage + "." + f.getName());
            }else{
                //如果不是我们java的class文件时退出循环
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

    /**
     * 把第一个字母转成大写
     * @param name
     * @return
     */
    private String toLowerFirstCase(String name){
        char[] chars = name.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
