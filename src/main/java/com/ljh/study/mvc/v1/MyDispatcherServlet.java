package com.ljh.study.mvc.v1;

import com.ljh.study.mvc.annotation.MyController;
import com.ljh.study.mvc.annotation.MyRequestMapping;
import com.ljh.study.mvc.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    //保存url和Method的对应关系
    private Map<String, Method> handlerMapping = new HashMap<String,Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception , Detail : " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //获取请求url
        String url = req.getRequestURI();
        //获取上下文url
        String contextPath = req.getContextPath();
        //把请求url和上下文的url拼接为
        url = (url + contextPath).replaceAll("/+","/");
        //如果在我们的handlerMapping中不存在此url，则404
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found !!!");
            return;
        }
        //通过url拿到对应的method方法
        Method method = this.handlerMapping.get(url);
        //通过方法拿到方法对应中的类
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        //从request中获取参数列表
        Map<String,String[]> params = req.getParameterMap();
        //执行对应的方法
        method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]});

    }

    //自定义servlet初始化
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件,先获得在xml中配置的key为contextConfigLocation的value值，进行配置文件加载
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2、扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3、初始化IOC，实例化所有相关的类
        doInstance();
        //4、完成DI操作，依赖注入
        doAutowired();
        //5、初始化HandlerMapping(把我们自定义的注解)
        initHandlerMapping();
        //进行匹配URL对应的controller和method方法然后执行对应的方法。
        System.out.println("MyDispatcherServlet is init..");
    }

    //初始化，把我们有controller注解和requestMapping修饰的类和方法的url组合起来，然后把对应的方法进去一个map中
    private void initHandlerMapping() {
        if(ioc.isEmpty()) return;
        //遍历我们已经实例化好ioc容器
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //通过反射获取class对象
            Class<?> clazz = entry.getValue().getClass();
            //判断是否有controller修饰，没有就忽略跳过
            if(!clazz.isAnnotationPresent(MyController.class)){continue;}
            //保存controller默认的url或者自定义的url
            String baseUrl = "";
            //判断是否有RequestMapping修饰
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                //有修饰则把url加上默认的路径或者是自定义的路径
                MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = myRequestMapping.value();
            }
            //获取该类的全部public方法
            for (Method method : clazz.getMethods()) {
                //判断是否有RequestMapping修饰，没有则忽略
                if(!method.isAnnotationPresent(MyRequestMapping.class)){continue;}
                //有修饰时我们则需要把上面controller的url和我们的方法url拼接起来，并且存入我们handlerMapping中
                MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = ("/" + baseUrl + myRequestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url,method);
                System.out.println("Mapped :" + url + "," + method);
            }
        }

    }

    //DI，把我们ioc容器中所有的到类中有Autowired修饰的属性赋值实例化
    private void doAutowired() {
        //对ioc容器判空
        if(ioc.isEmpty()) return;
        //遍历我们ioc的容器，拿到每个实例中的所有属性
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //通过反射获取到每个实例的全部属性（getDeclaredFields是获取全部属性，不管私有还有公有）
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                //判断属性是否有MyRequestMapping修饰
                if(!field.isAnnotationPresent(MyRequestMapping.class))continue;
                //有修饰时，拿到这个修饰注解的值，用于判断用户是否有自定义的名字
                MyRequestMapping annotation = field.getAnnotation(MyRequestMapping.class);
                //获取annotation的value值
                String beanName = annotation.value().trim();
                //判断是否等于""，如果等于则用户没有自己家，不等于就是加了
                if("".equals(annotation.value())){
                    beanName = field.getType().getName();
                }
                //设置私有属性的访问属性
                field.setAccessible(true);
                try {
                    //给属性实例化
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

    }

    //IOC，把我们通过扫描包下面全部的类有我们MyController、MyService的注解修饰的类实例化，存入ioc容器中
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
                if(!f.getName().endsWith(".class"))continue;
                //获取我们的文件类名
                String className = (scanPackage + "." + f.getName()).replace(".class", "");
                //放入我们存储文件名的list集合中，放入内存中存储
                classNames.add(className);
            }
        }
    }

    //加载配置文件，加载进我们的Properties对象中
    private void doLoadConfig(String contextConfigLocation) {
        //加载这个资源
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            //加载进我们的Properties的对象中
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
     * 把第一个字母转成小写
     * @param name
     * @return
     */
    private String toLowerFirstCase(String name){
        char[] chars = name.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
