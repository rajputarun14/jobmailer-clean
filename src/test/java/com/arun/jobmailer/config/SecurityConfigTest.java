package com.arun.jobmailer.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytest",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "aws.s3.enabled=false",
        "APP_USER=arun",
        "APP_PASSWORD=jobmailer123"
})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void formLoginWorksWithStaticLoginPage() throws Exception {
        mockMvc.perform(formLogin("/login").user("arun").password("jobmailer123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }
}
