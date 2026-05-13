package com.arun.jobmailer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import com.arun.jobmailer.auth.JwtAuthenticationFilter;
import com.arun.jobmailer.auth.RateLimitFilter;
import com.arun.jobmailer.model.UserAccount;
import com.arun.jobmailer.repository.UserRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           RateLimitFilter rateLimitFilter) throws Exception {

        http
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    "/login",
                    "/register",
                    "/api/auth/login",
                    "/send",
                    "/ai/**",
                    "/uploadResume",
                    "/emails/*/followup",
                    "/admin/**",
                    "/logout"
            ))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login.html", "/register.html", "/css/**", "/js/**", "/favicon.ico", "/register", "/api/auth/login", "/track/**").permitAll()
                .requestMatchers("/admin.html", "/admin/**").hasRole("ADMIN")
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
            )
            .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                if (response.isCommitted()) {
                    return;
                }
                String uri = request.getRequestURI();
                String ctx = request.getContextPath() == null ? "" : request.getContextPath();
                boolean adminHtml = uri.equals(ctx + "/admin.html");
                if (adminHtml) {
                    String home = ctx.isEmpty() ? "/" : ctx + "/";
                    response.resetBuffer();
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType("text/html;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    String html = """
                            <!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Access denied</title></head><body>
                            <script>
                            alert("You don't have access to this page. Only administrators can access the admin panel.");
                            window.location.replace("%s");
                            </script>
                            <noscript><p>Access denied. <a href="%s">Go to home</a></p></noscript>
                            </body></html>
                            """.formatted(home, home);
                    response.getWriter().write(html);
                    response.getWriter().flush();
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
            }))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

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
