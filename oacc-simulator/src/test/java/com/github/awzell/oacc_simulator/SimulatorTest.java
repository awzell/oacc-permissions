package com.github.awzell.oacc_simulator;

import com.acciente.oacc.AccessControlContext;
import com.acciente.oacc.PasswordCredentials;
import com.acciente.oacc.Resource;
import com.acciente.oacc.Resources;
import com.acciente.oacc.helper.SQLAccessControlSystemResetUtil;
import com.acciente.oacc.sql.SQLAccessControlContextFactory;
import com.acciente.oacc.sql.SQLProfile;

import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;

import liquibase.integration.spring.SpringLiquibase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.SocketUtils;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

import static com.wix.mysql.distribution.Version.v5_6_latest;
import static java.util.Locale.ENGLISH;

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
    String schema = "OACCDB";

    SQLAccessControlSystemResetUtil.resetOACC(ds, schema, SYS_PASSWORD);

    ctx = SQLAccessControlContextFactory.getAccessControlContext(
      ds, schema, SQLProfile.MySQL_5_6_NON_RECURSIVE);
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
    private final int port = SocketUtils.findAvailableTcpPort();

    @Bean
    public DataSource dataSource() {
      Properties props = new Properties();

      props.put("sessionVariables", "sql_mode=NO_AUTO_VALUE_ON_ZERO");
      props.put("useCompression", "true");

      DriverManagerDataSource ds = new DriverManagerDataSource();

      ds.setConnectionProperties(props);
      ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
      ds.setUrl(String.format(ENGLISH, "jdbc:mysql://:%d/mysql", port));
      ds.setUsername("root");
      ds.setPassword("");

      return ds;
    }

    @Bean(destroyMethod = "stop")
    public EmbeddedMysql mysqld(MysqldConfig config) {
      return EmbeddedMysql.anEmbeddedMysql(config).start();
    }

    @Bean
    public MysqldConfig config() {
      return MysqldConfig.aMysqldConfig(v5_6_latest)
        .withPort(port)
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
