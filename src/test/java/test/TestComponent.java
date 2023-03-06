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
        PreparedStatement preparedStatement0 = connection.prepareStatement("select * from work limit 100");
        preparedStatement0.executeQuery();

        PreparedStatement preparedStatement1 = connection.prepareStatement("select * from work limit 120");
        preparedStatement1.executeQuery();

        PreparedStatement preparedStatement2 = connection.prepareStatement("select * from work limit 140");
        preparedStatement2.executeQuery();

        PreparedStatement preparedStatement3 = connection.prepareStatement("select * from work limit 180");
        preparedStatement3.executeQuery();

        preparedStatement0.close();
        preparedStatement1.close();
        preparedStatement2.close();
        preparedStatement3.close();
        connection.close();
    }
}
