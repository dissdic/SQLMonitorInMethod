package test;

import com.monitorSQLInMethod.MonitorSQLInMethod;
import com.zaxxer.hikari.HikariDataSource;
import org.jcp.xml.dsig.internal.dom.ApacheTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;

@Component
public class TestComponent {

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    public void query() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
        PreparedStatement preparedStatement = connection.prepareStatement("select * from work limit 10");
        preparedStatement.execute();
    }

    public void normalQuery(){
        jdbcTemplate.queryForList("select * from work limit 88");
    }

    @MonitorSQLInMethod
    public void testQuery() throws Exception{
        System.out.println("run");
        query();
        query();
//        jdbcTemplate.execute("insert into work (id) values (1119)");
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
