package com.vi.appointmentservice.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class DatabaseConnectionsConfiguration {

  @Value("${spring.datasource.url}")
  private String url;

  @Value("${spring.datasource.username}")
  private String username;

  @Value("${spring.datasource.password}")
  private String password;

  @Value("${spring.datasource.driver-class-name}")
  private String className;

  @Value("${calcom.database.url}")
  private String calcomDatabaseUrl;

  @Value("${calcom.database.username}")
  private String calcomDatabaseUsername;

  @Value("${calcom.database.password}")
  private String calcomDatabasePassword;

  @Value("${calcom.database.driverClass}")
  private String calcomDatabaseDriver;

  @Bean
  @Primary
  public DataSource dataSource() {
    return DataSourceBuilder.create().url(url).username(username).password(password)
        .driverClassName(className).build();
  }

  @Bean(name = "calcomDBDataSource")
  public DataSource calcomDBDataSource() {
    return DataSourceBuilder.create().url(calcomDatabaseUrl)
        .username(calcomDatabaseUsername).password(calcomDatabasePassword).driverClassName(calcomDatabaseDriver)
        .build();
  }

  @Bean(name = "calcomDBTemplate")
  public JdbcTemplate calcomDBTemplate(
      @Qualifier("calcomDBDataSource") DataSource calcomDBDataSource) {
    var template = new JdbcTemplate();
    template.setDataSource(calcomDBDataSource);
    return template;
  }

  @Bean(name = "calcomDBNamedParamterTemplate")
  public NamedParameterJdbcTemplate calcomDBNamedParamterTemplate(
      @Qualifier("calcomDBDataSource") DataSource calcomDBDataSource) {
    return new NamedParameterJdbcTemplate(calcomDBDataSource);
  }

}