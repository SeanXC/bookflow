package com.bookflow.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.auth.AuthService;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.UserRepository;

@SpringBootTest(properties = {
	"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters",
	"bookflow.ai.llm.api-key="
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AssistantUnconfiguredIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private StaffRepository staffRepository;

	@Autowired
	private ServiceRepository serviceRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;

	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		appointmentRepository.deleteAllInBatch();
		staffRepository.deleteAllInBatch();
		serviceRepository.deleteAllInBatch();
		customerRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
		tenantRepository.deleteAllInBatch();
	}

	@Test
	void publicAssistantDoesNotCallAPaidLlmWhenNoApiKeyIsConfigured() throws Exception {
		authService.register(new RegisterRequest(
				"Glow Studio",
				"hello@example.com",
				null,
				"owner@example.com",
				"password123"));

		mockMvc.perform(post("/api/public/businesses/{slug}/assistant", "glow-studio")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "messages": [
						    { "role": "USER", "content": "Book a haircut" }
						  ]
						}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"))
			.andExpect(jsonPath("$.message").value("The booking assistant is not configured."));

		assertEquals(0, appointmentRepository.count());
	}
}
