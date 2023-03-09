package com.monitorSQLInMethod;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class MonitorSQLInMethodProcessor {

    public MonitorSQLInMethodProcessor(){
        System.out.println("init");
    }

    @Autowired
    private ApplicationContext applicationContext;

    Table<Field,Object,Object> modifyFields = HashBasedTable.create();
    Table<Field,Object,Object> originalFields = HashBasedTable.create();


    @Around("@annotation(com.monitorSQLInMethod.MonitorSQLInMethod)")
    public Object around(ProceedingJoinPoint point) throws Throwable{
        Collection<DataSource> cs = applicationContext.getBeansOfType(DataSource.class).values();
        if(cs.size()==0){
            return point.proceed();
        }

        dynamicReplaceDataSourceBean(cs);

        processConnectionHandle(cs);

        MethodSignature method_ = (MethodSignature)point.getSignature();
        Method method = method_.getMethod();

        MonitorSQLInMethodFactor factor = new MonitorSQLInMethodFactor();
        factor.setThread(Thread.currentThread().getName());
        factor.setMethod(method.getDeclaringClass().getName()+":"+method.getName());
        factor.setInvokeTime(OffsetDateTime.now());

        FactorContext.threadLocalForFactor.set(factor);

        long startMs = System.currentTimeMillis();

        Object obj = point.proceed();

        reset();
        long stopMs = System.currentTimeMillis();
        factor.setInvokeDuration(stopMs-startMs);

        MonitorSQLInMethodEvent event = new MonitorSQLInMethodEvent(factor);
        applicationContext.publishEvent(event);
        return obj;
    }

    private void processConnectionHandle(Collection<DataSource> dataSources){
        for (DataSource dataSource : dataSources) {
            ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
            if(conHolder != null && conHolder.getConnectionHandle()!=null) {
                ConnectionHandle handle = conHolder.getConnectionHandle();
                Connection conn = handle.getConnection();
                Connection connection = (Connection) Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class<?>[]{Connection.class},new ProxyConnection(conn));
                Field field = ReflectionUtils.findField(handle.getClass(),"connection");
                if(field!=null){
                    field.setAccessible(true);
                    ReflectionUtils.setField(field,handle,connection);
                }
            }
        }

    }

    private void reset() throws Exception{

        for (Table.Cell<Field, Object,Object> entry : originalFields.cellSet()) {
            entry.getRowKey().set(entry.getColumnKey(), entry.getValue());
        }
        FactorContext.threadLocalForFactor.remove();
    }

    private Object getTarget(Object obj) throws Exception {
        if(AopUtils.isAopProxy(obj)){
            if(!AopUtils.isJdkDynamicProxy(obj)){
                Field f = obj.getClass().getDeclaredField("CGLIB$CALLBACK_0");
                f.setAccessible(true);
                Object interceptor = f.get(obj);
                Field target = interceptor.getClass().getDeclaredField("advised");
                target.setAccessible(true);
                return ((AdvisedSupport)target.get(interceptor)).getTargetSource().getTarget();
            }else{
                Field f = obj.getClass().getDeclaredField("h");
                f.setAccessible(true);
                AopProxy aopProxy = (AopProxy)f.get(obj);
                Field target = aopProxy.getClass().getDeclaredField("advised");
                target.setAccessible(true);
                return ((AdvisedSupport)target.get(aopProxy)).getTargetSource().getTarget();
            }
        }
        return obj;
    }

    private Field findDataSourceField(Class<?> clazz){
        if(clazz==null){
            return null;
        }
        Field[] fs = clazz.getDeclaredFields();
        for (Field f : fs) {
            if(isDataSource(f.getType())){
                return f;
            }
        }
        return findDataSourceField(clazz.getSuperclass());
    }


    private boolean isDataSource(Class<?> clazz){
        if(clazz==null){
            return false;
        }
        if("DataSource".equalsIgnoreCase(clazz.getSimpleName())){
            return true;
        }
        Class<?>[] inters = clazz.getInterfaces();
        for (Class<?> inter : inters) {
            if(isDataSource(inter)){
                return true;
            }
        }
        Class<?> parent =  clazz.getSuperclass();
        if(isDataSource(parent)){
            return true;
        }
        return false;
    }

    /*
        一个类型的bean可能有多个，所以应根据对象实际用到的bean来做代理
     */
    private void dynamicReplaceDataSourceBean(Collection<DataSource> dataSources) throws Exception{

        if(modifyFields!=null && !modifyFields.isEmpty()){
            for (Table.Cell<Field, Object,Object> entry : modifyFields.cellSet()) {
                entry.getRowKey().set(entry.getColumnKey(), entry.getValue());
            }
            return;
        }
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)applicationContext.getAutowireCapableBeanFactory();
        Field singleton = DefaultSingletonBeanRegistry.class.getDeclaredField("singletonObjects");
        singleton.setAccessible(true);
        ConcurrentHashMap<String, Object> singletonObjects = (ConcurrentHashMap<String, Object>)singleton.get(beanFactory);

        for (Object value : singletonObjects.values()) {
            if(value instanceof MonitorSQLInMethodProcessor){
                continue;
            }

            value = getTarget(value);
            Field field = findDataSourceField(value.getClass());
            if(field!=null){
                field.setAccessible(true);
                if("DataSource".equalsIgnoreCase(field.getType().getSimpleName())){

                    Object origin = field.get(value);
                    if(origin!=null){
                        for (DataSource dataSource : dataSources) {
                            if(dataSource == origin){
                                Object proxyDataSource = Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class<?>[]{DataSource.class},new ProxyDataSource(dataSource));
                                originalFields.put(field,value,origin);
                                field.set(value,proxyDataSource);
                                modifyFields.put(field,value,proxyDataSource);
                                break;
                            }
                        }

                    }


                }else{
                    Object origin = field.get(value);
                    if(origin!=null){

                        for (DataSource dataSource : dataSources) {
                            if(dataSource==origin){
                                Enhancer enhancer = new Enhancer();
                                enhancer.setSuperclass(field.getType());
                                enhancer.setCallback(new ProxyDataSourceInterceptor(dataSource));
                                Object obj = enhancer.create();
                                originalFields.put(field,value,origin);
                                field.set(value,obj);
                                modifyFields.put(field,value,obj);
                                break;
                            }
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
                MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                if(factor==null){
                    return method.invoke(dataSource,args);
                }
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
            MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
            if(factor==null){
                return method.invoke(connection,args);
            }
            if("prepareStatement".equalsIgnoreCase(methodName)){
                String sql = (String)args[0];

                MonitorSQLInMethodFactor.SqlInfo sqlInfo = new MonitorSQLInMethodFactor.SqlInfo();
                sqlInfo.setSql(sql);
                factor.getSqlInfoList().offer(sqlInfo);

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
                if(args!=null && args.length>0){
                    String sql = (String)args[0];
                    MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                    MonitorSQLInMethodFactor.SqlInfo sqlInfo = new MonitorSQLInMethodFactor.SqlInfo();
                    //插到队首
                    factor.getSqlInfoList().push(sqlInfo);
                    sqlInfo.setSql(sql);
                    sqlInfo.setExecuteTime(OffsetDateTime.now());
                    long startMs = System.currentTimeMillis();
                    Object obj = method.invoke(preparedStatement,args);
                    long stopMs = System.currentTimeMillis();
                    sqlInfo.setMillSeconds(stopMs-startMs);
                    return obj;
                }else{
                    MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                    Deque<MonitorSQLInMethodFactor.SqlInfo> list = factor.getSqlInfoList();
                    MonitorSQLInMethodFactor.SqlInfo info = list.peekLast();
                    if(info!=null && info.getExecuteTime()==null){
                        info.setExecuteTime(OffsetDateTime.now());
                        long startMs = System.currentTimeMillis();
                        Object obj = method.invoke(preparedStatement,args);
                        long stopMs = System.currentTimeMillis();
                        info.setMillSeconds(stopMs-startMs);
                        return obj;
                    }
                }
            }

            return method.invoke(preparedStatement,args);
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
            if(methodName.contains("execute")){
                if(args!=null && args.length>0){
                    String sql = (String)args[0];
                    MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                    MonitorSQLInMethodFactor.SqlInfo sqlInfo = new MonitorSQLInMethodFactor.SqlInfo();
                    //插到队首
                    factor.getSqlInfoList().push(sqlInfo);
                    sqlInfo.setSql(sql);
                    sqlInfo.setExecuteTime(OffsetDateTime.now());
                    long startMs = System.currentTimeMillis();
                    Object obj = method.invoke(statement,args);
                    long stopMs = System.currentTimeMillis();
                    sqlInfo.setMillSeconds(stopMs-startMs);
                    return obj;
                }
            }
            return method.invoke(statement,args);
        }
    }

    public static class ProxyDataSourceInterceptor implements MethodInterceptor {

        private DataSource dataSource;

        public ProxyDataSourceInterceptor(DataSource dataSource){
            this.dataSource = dataSource;
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

            String methodName = method.getName();

            if("getConnection".equalsIgnoreCase(methodName)){
                MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                if(factor==null){
                    return method.invoke(dataSource,objects);
                }
                Object conn = method.invoke(dataSource,objects);
                if(Modifier.isFinal(conn.getClass().getModifiers())){
                    //使用JDK代理
                    ProxyConnection connection = new ProxyConnection((Connection) conn);
                    return Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class[]{Connection.class},connection);
                }
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(conn.getClass());
                enhancer.setCallback(new ProxyConnectionInterceptor((Connection) conn));
                return enhancer.create();
            }
            return method.invoke(dataSource,objects);
        }
    }

    public static class ProxyConnectionInterceptor implements MethodInterceptor {

        private Connection connection;

        public ProxyConnectionInterceptor(Connection connection){

            this.connection = connection;
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

            String methodName = method.getName();
            if("prepareStatement".equalsIgnoreCase(methodName)){
                String sql = (String)objects[0];
                MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                MonitorSQLInMethodFactor.SqlInfo sqlInfo = new MonitorSQLInMethodFactor.SqlInfo();
                sqlInfo.setSql(sql);
                factor.getSqlInfoList().offer(sqlInfo);


                Object obj = method.invoke(connection,objects);

                if(Modifier.isFinal(obj.getClass().getModifiers())){
                    ProxyPrepareStatement proxyPreparedStatement = new ProxyPrepareStatement((PreparedStatement) obj);
                    return Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},proxyPreparedStatement);
                }
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(obj.getClass());
                enhancer.setCallback(new ProxyPreparedStatementInterceptor((PreparedStatement) obj));

                return enhancer.create();
            }
            if("createStatement".equalsIgnoreCase(methodName)){

                Object obj = method.invoke(connection,objects);
                if(Modifier.isFinal(obj.getClass().getModifiers())){
                    ProxyStatement proxyStatement = new ProxyStatement((Statement) obj);
                    return Proxy.newProxyInstance(MonitorSQLInMethodProcessor.class.getClassLoader(),new Class<?>[]{Statement.class},proxyStatement);
                }
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(obj.getClass());
                enhancer.setCallback(new ProxyStatementInterceptor((Statement) obj));

                return enhancer.create();
            }
            return method.invoke(connection,objects);
        }
    }

    public static class ProxyPreparedStatementInterceptor implements MethodInterceptor {

        private PreparedStatement preparedStatement;

        public ProxyPreparedStatementInterceptor(PreparedStatement preparedStatement){
            this.preparedStatement = preparedStatement;
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            String methodName = method.getName();
            if(methodName.contains("execute")){
                if(objects!=null && objects.length>0){
                    String sql = (String)objects[0];
                    MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                    MonitorSQLInMethodFactor.SqlInfo sqlInfo = new MonitorSQLInMethodFactor.SqlInfo();
                    //插到队首
                    factor.getSqlInfoList().push(sqlInfo);
                    sqlInfo.setSql(sql);
                    sqlInfo.setExecuteTime(OffsetDateTime.now());
                    long startMs = System.currentTimeMillis();
                    Object obj = method.invoke(preparedStatement,objects);
                    long stopMs = System.currentTimeMillis();
                    sqlInfo.setMillSeconds(stopMs-startMs);
                    return obj;
                }else{
                    MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                    Deque<MonitorSQLInMethodFactor.SqlInfo> list = factor.getSqlInfoList();
                    MonitorSQLInMethodFactor.SqlInfo info = list.peekLast();
                    if(info!=null && info.getExecuteTime()==null){
                        info.setExecuteTime(OffsetDateTime.now());
                        long startMs = System.currentTimeMillis();
                        Object obj = method.invoke(preparedStatement,objects);
                        long stopMs = System.currentTimeMillis();
                        info.setMillSeconds(stopMs-startMs);
                        return obj;
                    }
                }
            }

            return method.invoke(preparedStatement,objects);
        }
    }

    public static class ProxyStatementInterceptor implements MethodInterceptor {

        private Statement statement;

        public ProxyStatementInterceptor(Statement statement){
            this.statement = statement;
        }
        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            String methodName = method.getName();
            if(methodName.contains("execute")){
                if(objects!=null && objects.length>0){
                    String sql = (String)objects[0];
                    MonitorSQLInMethodFactor factor = FactorContext.threadLocalForFactor.get();
                    MonitorSQLInMethodFactor.SqlInfo sqlInfo = new MonitorSQLInMethodFactor.SqlInfo();
                    //插到队首
                    factor.getSqlInfoList().push(sqlInfo);
                    sqlInfo.setSql(sql);
                    sqlInfo.setExecuteTime(OffsetDateTime.now());
                    long startMs = System.currentTimeMillis();
                    Object obj = method.invoke(statement,objects);
                    long stopMs = System.currentTimeMillis();
                    sqlInfo.setMillSeconds(stopMs-startMs);
                    return obj;
                }
            }
            return method.invoke(statement,objects);
        }
    }
}
