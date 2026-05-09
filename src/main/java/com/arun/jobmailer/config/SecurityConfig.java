package com.arun.jobmailer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.arun.jobmailer.model.UserAccount;
import com.arun.jobmailer.repository.UserRepository;

@Configuration
public class SecurityConfig {

    @Value("${APP_USER:arun}")
    private String appUser;

    @Value("${APP_PASSWORD:jobmailer123}")
    private String appPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login.html", "/register.html", "/css/**", "/js/**", "/favicon.ico", "/register").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepo) {
        return username -> {
            UserAccount u = userRepo.findById(username).orElse(null);
            if (u == null) throw new UsernameNotFoundException("user not found");
            return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                    .password(u.getPassword())
                    .roles(u.getRoles() == null ? new String[]{"USER"} : u.getRoles().split(","))
                    .build();
        };
    }
}