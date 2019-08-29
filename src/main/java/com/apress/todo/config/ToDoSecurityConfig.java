package com.apress.todo.config;

import com.apress.todo.directory.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@EnableWebSecurity
//https://docs.spring.io/spring-security/site/docs/4.2.x/reference/htmlsingle/#multiple-httpsecurity
/**
 * WebSecurityConfigurerAdapter. Extending this class is one way to override security because it allows you to override
 * the methods that you really need. In this case, the code overrides the configure(AuthenticationManagerBuilder) signature.
 **/
@EnableConfigurationProperties(ToDoProperties.class)
@Configuration
public class ToDoSecurityConfig {

  private final Logger log = LoggerFactory.getLogger(ToDoSecurityConfig.class);
  //Use this to connect to the Directory App
  private RestTemplate restTemplate;
  private ToDoProperties toDoProperties;
  private UriComponentsBuilder builder;
  public ToDoSecurityConfig(RestTemplateBuilder restTemplateBuilder, ToDoProperties toDoProperties){
    this.toDoProperties = toDoProperties;
    this.restTemplate = restTemplateBuilder.basicAuthentication(toDoProperties.getUsername(), toDoProperties.getPassword()).build();
  }
 // @Override
  /**
   * AuthenticationManagerBuilder. This class creates an AuthenticationManager that allows you to easily build in memory,
   * LDAP, JDBC authentications, UserDetailsService and add AutheticationProviders. In this case, you are building
   * an in-memory authentication. It’s necessary to add a PasswordEncoder
   *
   * If there is no PasswordEncoder you might need to use "{noop}password")
   */
  /*protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.inMemoryAuthentication()
        .passwordEncoder(passwordEncoder())
        .withUser("apress")
        .password(passwordEncoder().encode("springboot2"))
        .roles("ADMIN","USER");
  }*/

  @Bean
  public UserDetailsService userDetailsService() throws Exception {
    /*InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    manager.createUser(User.withUsername("apress").password(passwordEncoder().encode("springboot2")).roles("USER","ADMIN").build());
    manager.createUser(User.withUsername("admin").password(passwordEncoder().encode("password")).roles("USER","ADMIN").build());
    return manager;*/
    return new UserDetailsService() {
      @Override
      public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
          builder = UriComponentsBuilder
              .fromUriString(toDoProperties.getFindByEmailUri())
              .queryParam("email", username);
          log.info("Querying: " + builder.toUriString());
          ResponseEntity<Resource<Person>> responseEntity =
              restTemplate.exchange(
                  RequestEntity.get(URI.create(builder.toUriString()))
                      .accept(MediaTypes.HAL_JSON)
                      .build()
                  , new ParameterizedTypeReference<Resource<Person>>() {
                  });
          if (responseEntity.getStatusCode() == HttpStatus.OK) {
            Resource<Person> resource = responseEntity.getBody();
            Person person = resource.getContent();
            PasswordEncoder encoder = passwordEncoder();
                //PasswordEncoderFactories.createDelegatingPasswordEncoder();
            String password = encoder.encode(person.getPassword());
            return User
                .withUsername(person.getEmail())
                .password(password)
                .accountLocked(!person.isEnabled())
                .roles(person.getRole()).build();
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        throw new UsernameNotFoundException(username);
      }
    };
  }

  @Bean
  /**
   * BCryptPasswordEncoder. In this code you are using the BCryptPasswordEncoder (returns a PasswordEncoder implementation)
   * that uses the BCrypt strong hashing function. You can use also Pbkdf2PasswordEncoder (uses PBKDF2 with a configurable
   * number of iterations and a random 8-byte random salt value), or SCryptPasswordEncoder (uses the SCrypt hashing function).
   * Even better, use DelegatingPasswordEncoder, which supports password upgrades.
   */
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  //@Override
  /**
   * Spring Security allows you to override the default login page in several ways. One way is to configure HttpSecurity.
   * By default, it is applied to all requests, but can be restricted using requestMatcher(RequestMatcher) or similar methods
   * curl localhost:8080/api/toDos -u apress:springboot2
   */
  /*protected void configure(HttpSecurity http) throws Exception {
    http
        .authorizeRequests()
        .requestMatchers(//point to common locations, such as the static resources (static/* ). This is where CSS, JS, or any other simple HTML can live and doesn’t need any security
            PathRequest
                .toStaticResources()
                .atCommonLocations()).permitAll()
        .anyRequest().fullyAuthenticated()//uses anyRequest, which should be fullyAuthenticated this means that the /api/*
        .and()
        .formLogin().loginPage("/login").permitAll()
        .and()
        .logout()
        .logoutRequestMatcher( new AntPathRequestMatcher("/logout"))
        .logoutSuccessUrl("/login");
       *//* .and()
        .httpBasic();*//*
  }*/
  @Configuration
  @Order(1)
  public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
    protected void configure(HttpSecurity http) throws Exception {
      http
          .antMatcher("/api/**")
          .authorizeRequests()
          .anyRequest().hasRole("USER")
          .and()
          .httpBasic();
    }
  }

  @Configuration
  public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .authorizeRequests()
          .requestMatchers(//point to common locations, such as the static resources (static/* ). This is where CSS, JS, or any other simple HTML can live and doesn’t need any security
              PathRequest
                  .toStaticResources()
                  .atCommonLocations()).permitAll()
          .anyRequest().fullyAuthenticated()//uses anyRequest, which should be fullyAuthenticated this means that the /api/*
          .antMatchers("/","/api/**").hasRole("USER")
          .and()
          .formLogin().loginPage("/login").permitAll()
          .and()
          .logout()
          .logoutRequestMatcher( new AntPathRequestMatcher("/logout"))
          .logoutSuccessUrl("/login");
    }
  }
}
