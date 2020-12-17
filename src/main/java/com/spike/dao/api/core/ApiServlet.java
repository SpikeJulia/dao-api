package com.spike.dao.api.core;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import javax.servlet.http.HttpServlet;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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

    private void loadApi() {
        String[] names = applicationContext.getBeanDefinitionNames();
    }

    private class ApiRunnable {
        Method method;
        Object target;
        String beanName;

        public Object run(Object[] args) throws InvocationTargetException, IllegalAccessException {
            if (target == null) {
                target = applicationContext.getBean(beanName);
            }
            return method.invoke(target, args);
        }
    }
}
