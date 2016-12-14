package com.capitalone.dashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.capitalone.dashboard.auth.JWTAuthenticationFilter;
import com.capitalone.dashboard.auth.JWTLoginFilter;

@Configuration
 @EnableWebSecurity
 public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

     @Override
     protected void configure(HttpSecurity http) throws Exception {
         http.headers().cacheControl();
         http.csrf();
         http.authorizeRequests()
             .antMatchers("/appinfo").permitAll()
             .antMatchers("/authenticateUser").permitAll()
             .anyRequest().authenticated()
             .and()
             .addFilterBefore(new JWTLoginFilter("/authenticateUser", authenticationManager()), UsernamePasswordAuthenticationFilter.class)
             .addFilterBefore(new JWTAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
     }
     
     @Override
     protected void configure(AuthenticationManagerBuilder auth) throws Exception {
         auth.inMemoryAuthentication()
             .withUser("admin")
             .password("password")
             .roles("ADMIN");
     }
}