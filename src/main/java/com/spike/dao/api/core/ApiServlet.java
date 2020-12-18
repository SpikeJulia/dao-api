package com.spike.dao.api.core;

import com.spike.dao.api.util.UtilJson;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author tangxuan
 * @date 2020/12/17 21:31
 */

public class ApiServlet extends HttpServlet implements InitializingBean {
    ApplicationContext applicationContext;
    Map<String, ApiRunnable> registers = new HashMap<>();
    ParameterNameDiscoverer nameDiscoverer;

    public ApiServlet(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        loadApi();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doDispatch(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        String method = req.getParameter("method");
        String parameter = req.getParameter("parameter");
        Assert.hasText(method, "method can't be null");
        Assert.hasText(parameter, "parameter can't be null");
        Assert.isTrue(registers.containsKey(method), "method is not exist");
        ApiRunnable apiRunnable = registers.get(method);
        Object[] args = buildParam(apiRunnable, parameter);
        try {
            Object result = apiRunnable.run(args);
            // 对结果进行封装
            writeResult(result, resp);
        } catch (InvocationTargetException | IOException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private void writeResult(Object result, HttpServletResponse resp) throws IOException {
        if (result != null) {
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html/json;charset=utf-8");
            resp.setHeader("Cache-Control", "no-cache");
            resp.setHeader("Pragma","no-cache");
            resp.setDateHeader("Expires", 0);
            resp.getWriter().write(Objects.requireNonNull(UtilJson.writeValueAsString(result)));
        }
    }

    private Object[] buildParam(ApiRunnable apiRunnable, String parameter) {
        // 获取参数类别
        Class<?>[] types = apiRunnable.method.getParameterTypes();
        // 获取方法名称
        String[] names = nameDiscoverer.getParameterNames(apiRunnable.method);
        Object[] args = new Object[names.length];

        // 转换成map
        Map<String, Object> map = UtilJson.toMap(parameter);
        for (int i = 0; i < names.length; i++) {
            if (!map.containsKey(names[i])) {
                continue;
            }
            // 不存在则业务异常处理
            Object arg = UtilJson.convertValue(map.get(names[i]), types[i]);
            args[i] = arg;
        }
        return args;
    }

    /**
     * 装载API
     */
    private void loadApi() {
        String[] names = applicationContext.getBeanDefinitionNames();
        // 拿到所有的类名
        for (String name : names) {
            // 通过类名获取类型
            Class<?> type = applicationContext.getType(name);
            // 通过类型拿到方法，判断方法上是否有apiMapping注解
            for (Method method : type.getDeclaredMethods()) {
                ApiMapping mapping = method.getAnnotation(ApiMapping.class);
                // 包装api，放到集合中（注册），统一处理
                registerApi(name, method, mapping);

            }
        }
    }

    // 注册一个api
    private void registerApi(String beanName, Method method, ApiMapping mapping) {
        ApiRunnable api = new ApiRunnable();
        api.method = method;
        api.beanName = beanName;
        api.target = applicationContext.getBean(beanName);
        registers.put(mapping.value(), api);
    }

    private class ApiRunnable {
        Method method;
        Object target;
        String beanName;

        public Object run(Object[] args) throws InvocationTargetException, IllegalAccessException {
            if (target == null) {
                // 并发问题？IOC在单例创建时就已经线程同步
                target = applicationContext.getBean(beanName);
            }
            return method.invoke(target, args);
        }
    }
}
