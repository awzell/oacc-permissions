package com.github.awzell.oacc_simulator;

import javax.inject.Inject;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import org.testng.annotations.Test;

@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@Test
public class SimulatorTest extends AbstractTestNGSpringContextTests {
  @Inject
  private DataSource ds;

  public void test() {
    logger().info("DataSource: {}", ds);
  }

  private Logger logger() {
    return LoggerFactory.getLogger(SimulatorTest.class);
  }

  @Configuration
  static class ContextConfiguration {
    @Bean
    public DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()
        .setName("oaccdb")
        .setType(EmbeddedDatabaseType.HSQL)
        .build();
    }

    @Bean
    public SpringLiquibase springLiquibase(DataSource ds) {
      SpringLiquibase obj = new SpringLiquibase();

      obj.setDataSource(ds);
      obj.setChangeLog("classpath:oaccdb.changelog-master.xml");

      return obj;
    }
  }
}
