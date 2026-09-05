package com.bookflow.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
