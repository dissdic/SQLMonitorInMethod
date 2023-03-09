package test;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

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
        dataSourceTransactionManager.setDataSource(dataSource);
        return dataSourceTransactionManager;
    }


}
