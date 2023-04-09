package com.example.pagingdemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 1) Master/Slave DataSource 정의 (masterDataSource(), slaveDataSource())
 * 2) Master/Slave DB 분기 처리를 위한 사전 세팅 (createRoutingDataSource())
 *
 * @author : jshan
 * @created : 2023/04/04
 */
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(MybatisProperties.class)
@MapperScan(basePackages = {"com.example.pagingdemo.mapper", "com.example.pagingdemo.repository"})
@Configuration
public class DataSourceConfig
{

    private final MybatisProperties mybatisProperties;
    private final MybatisInterceptor mybatisInterceptor;

    @Bean("beanTestDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);

        log.info("================ factory");

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(mybatisProperties.getConfiguration().isMapUnderscoreToCamelCase());
        configuration.setLogImpl(mybatisProperties.getConfiguration().getLogImpl());

        sessionFactoryBean.setPlugins(mybatisInterceptor);
        sessionFactoryBean.setTypeAliasesPackage(mybatisProperties.getTypeAliasesPackage());
        sessionFactoryBean.setMapperLocations(mybatisProperties.resolveMapperLocations());
        sessionFactoryBean.setConfiguration(configuration);

        return sessionFactoryBean.getObject();
    }

    @DependsOn("beanTestDatasource")
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactionManager.setGlobalRollbackOnParticipationFailure(false);
        return transactionManager;
    }
}
