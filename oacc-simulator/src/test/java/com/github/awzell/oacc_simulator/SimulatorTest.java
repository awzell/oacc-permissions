package com.github.awzell.oacc_simulator;

import com.acciente.oacc.AccessControlContext;
import com.acciente.oacc.PasswordCredentials;
import com.acciente.oacc.Resource;
import com.acciente.oacc.Resources;
import com.acciente.oacc.helper.SQLAccessControlSystemResetUtil;
import com.acciente.oacc.sql.SQLAccessControlContextFactory;
import com.acciente.oacc.sql.SQLProfile;

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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.SQLException;

@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@Test
public class SimulatorTest extends AbstractTestNGSpringContextTests {
  private static final char[] SYS_PASSWORD = "toomanysecrets".toCharArray();
  private static final Resource SYS_RESOURCE = Resources.getInstance(0);
  @Inject
  private DataSource ds;
  private AccessControlContext ctx;

  @AfterMethod
  public void afterMethod() {
    ctx.unauthenticate();
  }

  @BeforeMethod
  public void beforeMethod() throws SQLException {
    String schema = "OACC";

    SQLAccessControlSystemResetUtil.resetOACC(ds, schema, SYS_PASSWORD);

    ctx = SQLAccessControlContextFactory.getAccessControlContext(
      ds, schema, SQLProfile.HSQLDB_2_3_NON_RECURSIVE);
  }

  private Logger logger() {
    return LoggerFactory.getLogger(SimulatorTest.class);
  }

  public void test() {
    ctx.authenticate(
      SYS_RESOURCE, PasswordCredentials.newInstance(SYS_PASSWORD));

    logger().info("authenticated successfully");
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
