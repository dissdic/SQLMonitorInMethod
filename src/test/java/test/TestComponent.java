package test;

import com.monitorSQLInMethod.MonitorSQLInMethod;
import com.zaxxer.hikari.HikariDataSource;
import org.jcp.xml.dsig.internal.dom.ApacheTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Component
public class TestComponent {

    @Autowired
    private HikariDataSource hikariDataSource;

    @MonitorSQLInMethod
    public void test() throws Exception{

        Connection connection = hikariDataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("select * from work limit 100");
        preparedStatement.executeQuery();
        preparedStatement.close();
        connection.close();
    }
}
