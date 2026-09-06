package com.bookflow.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
		"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters"
})
@Import(TestcontainersConfiguration.class)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
