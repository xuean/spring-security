package com.apress.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ToDoWebConfig implements WebMvcConfigurer {
  @Override
  /**
   * tell Spring MVC how to locate the login page.
   * you can still use a class annotated with @Controller and create the mapping for the login page;
   * but this is the JavaConfig way
   */
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/login").setViewName("login");
  }
}
