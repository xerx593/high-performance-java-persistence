package com.vladmihalcea.book.hpjp.spring.transaction.hibernate.config;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.logging.LoggingStatementInspector;
import com.vladmihalcea.book.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.book.hpjp.util.logging.InlineQueryLogEntryCreator;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@Configuration
@ComponentScan(basePackages = "com.vladmihalcea.book.hpjp.spring.transaction.hibernate")
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class HibernateTransactionManagerConfiguration {

    public static final String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

    @Bean
    public Database database() {
        return Database.POSTGRESQL;
    }

    @Bean
    public DataSourceProvider dataSourceProvider() {
        return database().dataSourceProvider();
    }

    @Bean(destroyMethod = "close")
    public HikariDataSource actualDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(64);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setDataSource(dataSourceProvider().dataSource());
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public DataSource dataSource() {
        SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
        loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
        DataSource dataSource = ProxyDataSourceBuilder
            .create(actualDataSource())
            .name(DATA_SOURCE_PROXY_NAME)
            .listener(loggingListener)
            .build();
        return dataSource;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        sessionFactoryBean.setPackagesToScan(packagesToScan());
        sessionFactoryBean.setHibernateProperties(additionalProperties());
        sessionFactoryBean.setHibernateIntegrators(new ClassImportIntegrator(Arrays.asList(PostDTO.class)));
        return sessionFactoryBean;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory){
        //HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        HibernateTransactionManager transactionManager = new MonitoringHibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory);
        return transactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate(SessionFactory sessionFactory) {
        return new TransactionTemplate(transactionManager(sessionFactory));
    }

    protected Properties additionalProperties() {
        Properties properties = new Properties();
        
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.put(
            "hibernate.session_factory.statement_inspector",
            new LoggingStatementInspector("com.vladmihalcea.book.hpjp.hibernate.transaction")
        );
        return properties;
    }

    protected String[] packagesToScan() {
        return new String[]{
            "com.vladmihalcea.book.hpjp.hibernate.transaction.forum"
        };
    }
}
