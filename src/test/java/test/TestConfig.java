package test;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Method;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(value = {"com.monitorSQLInMethod","test"})
public class TestConfig {

    @Bean
    public HikariDataSource dataSource(){
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setUsername("postgres");
        hikariDataSource.setPassword("123456");
        hikariDataSource.setJdbcUrl("jdbc:postgresql://localhost:5433/postgres");
        hikariDataSource.setDriverClassName("org.postgresql.Driver");
        return hikariDataSource;
    }
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource){

        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager(DataSource dataSource){
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();

        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(new DataSourceInterceptor(dataSource));
        enhancer.setSuperclass(DataSource.class);

        dataSourceTransactionManager.setDataSource((DataSource) enhancer.create());
        return dataSourceTransactionManager;
    }

    public static class DataSourceInterceptor implements MethodInterceptor {

        private DataSource dataSource;

        public DataSourceInterceptor(DataSource dataSource){
            this.dataSource = dataSource;
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            System.out.println("exterior");
            return method.invoke(dataSource,objects);
        }
    }
}
