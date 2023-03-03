package test;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

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
}
