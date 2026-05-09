package com.arun.jobmailer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:testdb",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"aws.s3.enabled=false"
})
class JobmailerApplicationTests {

	@Test
	void contextLoads() {
	}

}
