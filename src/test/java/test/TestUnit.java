package test;

import com.monitorSQLInMethod.MonitorSQLInMethodProcessor;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@ContextConfiguration(classes = {TestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({TransactionalTestExecutionListener.class,DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
public class TestUnit {


    @Autowired
    private TestComponent testComponent;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Test
    public void test() throws Exception{
        testComponent.test();
    }
    @Test
    @Transactional
    public void test1() throws Exception {

        testComponent.normalQuery();

        testComponent.testQuery();

    }

    public void trans(){

        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(platformTransactionManager);
        transactionTemplate.execute(new TransactionCallback<Object>(){
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                testComponent.normalQuery();
                testComponent.testQuery();
                return null;
            }
        });
    }

    @Test
    public void test2(){
        trans();
    }



}
