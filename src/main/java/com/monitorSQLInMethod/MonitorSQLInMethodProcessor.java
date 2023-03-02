package com.monitorSQLInMethod;

import ch.qos.logback.core.pattern.FormatInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class MonitorSQLInMethodProcessor {

    @Autowired
    private ApplicationContext applicationContext;


    DataSource originDataSource;
    ConcurrentHashMap<String, Object> storedMap;
    String storedDataSourceBeanName;
    Map<Field,Object> modifyFields = new ConcurrentHashMap<>();


    @Around("@annotation(com.monitorSQLInMethod.MonitorSQLInMethod)")
    public Object around(ProceedingJoinPoint point) throws Throwable{
        System.out.println("start");
        Collection<DataSource> cs = applicationContext.getBeansOfType(DataSource.class).values();
        if(cs.size()==0){
            return point.proceed();
        }
        DataSource dataSource = new ArrayList<>(cs).get(0);
        ProxyDataSource proxyDataSource = new ProxyDataSource(dataSource);
        DataSource proxyDatasource = (DataSource) Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class[]{DataSource.class},proxyDataSource);
        //替换掉
        dynamicReplaceDataSourceBean(proxyDatasource);

        MethodSignature method_ = (MethodSignature)point.getSignature();
        Method method = method_.getMethod();

        MonitorSQLInMethodFactor factor = new MonitorSQLInMethodFactor();
        factor.setThread(Thread.currentThread().getName());
        factor.setMethod(method.getName());
        factor.setInvokeTime(OffsetDateTime.now());

        FactorContext.threadLocalForFactor.set(factor);

        long startMs = System.currentTimeMillis();

        Object obj = point.proceed();
        reset();
        long stopMs = System.currentTimeMillis();
        factor.setInvokeDuration(stopMs-startMs);
        //触发一个spring事件，让用户自己处理
        MonitorSQLInMethodEvent event = new MonitorSQLInMethodEvent(factor);
        applicationContext.publishEvent(event);
        System.out.println("stop");
        return obj;
    }

    private void reset() throws Exception{
        storedMap.put(storedDataSourceBeanName,originDataSource);
        for (Map.Entry<Field, Object> entry : modifyFields.entrySet()) {
            entry.getKey().set(entry.getValue(), originDataSource);
        }
    }

    private void dynamicReplaceDataSourceBean(DataSource dataSource) throws Exception{
        if(storedMap!=null){
            storedMap.put(storedDataSourceBeanName,dataSource);
            for (Map.Entry<Field, Object> entry : modifyFields.entrySet()) {
                entry.getKey().set(entry.getValue(), dataSource);
            }
            return;
        }
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)applicationContext.getAutowireCapableBeanFactory();
        String datasourceName = beanFactory.getBeanNamesForType(DataSource.class)[0];
        Field singleton = DefaultSingletonBeanRegistry.class.getDeclaredField("singletonObjects");
        singleton.setAccessible(true);
        ConcurrentHashMap<String, Object> singletonObjects = (ConcurrentHashMap<String, Object>)singleton.get(beanFactory);

        originDataSource = (DataSource) singletonObjects.get(datasourceName);
        storedMap = singletonObjects;
        storedDataSourceBeanName = datasourceName;

        singletonObjects.put(datasourceName,dataSource);

        for (Object value : singletonObjects.values()) {
            Field[] fs = value.getClass().getDeclaredFields();
            for (Field f : fs) {
                Class<?> cls = f.getType();
                if ("DataSource".equalsIgnoreCase(cls.getSimpleName())){
                    f.set(value,dataSource);
                    modifyFields.put(f,value);
                }else{
                    Class<?> superClass = cls.getSuperclass();
                    while (superClass!=null){
                        if("DataSource".equalsIgnoreCase(superClass.getSimpleName())){
                            f.set(value,dataSource);
                            modifyFields.put(f,value);
                            break;
                        }else{
                            superClass = superClass.getSuperclass();
                        }
                    }
                }
            }
        }
    }

    public static class FactorContext{
        public static ThreadLocal<MonitorSQLInMethodFactor> threadLocalForFactor = new ThreadLocal<>();
    }

    public static class ProxyDataSource implements InvocationHandler{

        private final DataSource dataSource;

        public ProxyDataSource(DataSource dataSource){
            this.dataSource = dataSource;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if("getConnection".equalsIgnoreCase(methodName)){
                Connection conn = (Connection) method.invoke(dataSource,args);
                ProxyConnection connection = new ProxyConnection(conn);
                return Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class[]{Connection.class},connection);
            }
            return method.invoke(dataSource,args);
        }
    }

    public static class ProxyConnection implements InvocationHandler {

        private final Connection connection;

        public ProxyConnection(Connection connection){
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            String methodName = method.getName();
            if("prepareStatement".equalsIgnoreCase(methodName)){
                String sql = (String)args[0];
                MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                MonitorSQLInMethodFactor.SqlInfo sqlInfo = new MonitorSQLInMethodFactor.SqlInfo();
                factor.getSqlInfoList().add(sqlInfo);

                sqlInfo.setSql(sql);

                PreparedStatement obj = (PreparedStatement)method.invoke(connection,args);
                ProxyPrepareStatement prepareStatement = new ProxyPrepareStatement(obj);
                return Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class[]{PreparedStatement.class},prepareStatement);
            }
            if("createStatement".equalsIgnoreCase(methodName)){

                Statement statement = (Statement)method.invoke(connection,args);
                ProxyStatement proxyStatement = new ProxyStatement(statement);
                return Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class[]{Statement.class},proxyStatement);
            }
            return method.invoke(connection,args);
        }
    }

    public static class ProxyPrepareStatement implements InvocationHandler{

        private PreparedStatement preparedStatement;

        public ProxyPrepareStatement(PreparedStatement preparedStatement){
            this.preparedStatement = preparedStatement;
        }
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            String methodName = method.getName();
            if(methodName.contains("execute")){
                if(args.length>0){
                    String sql = (String)args[0];
                    MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                    MonitorSQLInMethodFactor.SqlInfo sqlInfo = new MonitorSQLInMethodFactor.SqlInfo();
                    factor.getSqlInfoList().set(0,sqlInfo);

                    sqlInfo.setSql(sql);
                }else{

                }
            }

            return null;
        }
    }

    public static class ProxyStatement implements InvocationHandler {

        private Statement statement;

        public ProxyStatement(Statement statement){
            this.statement = statement;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            String methodName = method.getName();
            return null;
        }
    }

}
