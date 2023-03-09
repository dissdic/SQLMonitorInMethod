package test;

import com.monitorSQLInMethod.MonitorSQLInMethod;
import com.zaxxer.hikari.HikariDataSource;
import org.jcp.xml.dsig.internal.dom.ApacheTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.Callable;

@Component
public class TestComponent {

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public void normalQuery(){
        jdbcTemplate.queryForList("select * from work limit 88");
    }

    @MonitorSQLInMethod
    public void testQuery(){
        System.out.println("run");
        jdbcTemplate.queryForList("select * from work limit 100");
        jdbcTemplate.queryForList("select * from work limit 120");
        jdbcTemplate.queryForList("select * from work limit 140");
        jdbcTemplate.queryForList("select * from work limit 160");

        jdbcTemplate.execute("insert into work (id) values (1119)");
    }

    @MonitorSQLInMethod
    public void test() throws Exception{

        Connection connection = hikariDataSource.getConnection();
        PreparedStatement preparedStatement0 = connection.prepareStatement("select * from work limit 100");
        preparedStatement0.executeQuery();

        PreparedStatement preparedStatement1 = connection.prepareStatement("select * from work limit 120");
        preparedStatement1.executeQuery();

        PreparedStatement preparedStatement2 = connection.prepareStatement("select * from work limit 140");
        preparedStatement2.executeQuery();

        PreparedStatement preparedStatement3 = connection.prepareStatement("select * from work limit 160");
        preparedStatement3.executeQuery();

        preparedStatement0.close();
        preparedStatement1.close();
        preparedStatement2.close();
        preparedStatement3.close();
        connection.close();
    }
}
