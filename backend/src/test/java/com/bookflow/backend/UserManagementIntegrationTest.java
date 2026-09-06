package com.bookflow.backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.auth.AuthService;
import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.security.AuthenticatedUser;
import com.bookflow.backend.security.JwtTokenService;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.Role;
import com.bookflow.backend.user.User;
import com.bookflow.backend.user.UserRepository;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
	"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class UserManagementIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void ownerCanCreateListAndDisableAReceptionist() throws Exception {
		String ownerToken = registerOwner();

		MvcResult created = mockMvc.perform(post("/api/users")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "reception@example.com",
						  "password": "password123",
						  "role": "RECEPTIONIST"
						}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("reception@example.com"))
			.andExpect(jsonPath("$.role").value("RECEPTIONIST"))
			.andExpect(jsonPath("$.enabled").value(true))
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andReturn();
		long userId = jsonMapper.readTree(created.getResponse().getContentAsString())
				.get("id")
				.asLong();

		mockMvc.perform(get("/api/users")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2));

		mockMvc.perform(patch("/api/users/{userId}/enabled", userId)
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "enabled": false
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enabled").value(false));

		User disabled = userRepository.findById(userId).orElseThrow();
		assertFalse(disabled.isEnabled());
	}

	@Test
	void staffCannotManageUserAccounts() throws Exception {
		String staffToken = createTokenForRole(Role.STAFF);

		mockMvc.perform(get("/api/users")
				.header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void ownerCannotCreateAnotherOwnerThroughUserManagement() throws Exception {
		String ownerToken = registerOwner();

		mockMvc.perform(post("/api/users")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "another-owner@example.com",
						  "password": "password123",
						  "role": "OWNER"
						}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));
	}

	private String registerOwner() {
		AuthResponse response = authService.register(new RegisterRequest(
				"Glow Studio",
				"hello@example.com",
				null,
				"owner@example.com",
				"password123"));
		return response.accessToken();
	}

	private String createTokenForRole(Role role) {
		Tenant tenant = tenantRepository.saveAndFlush(new Tenant(
				"Glow Studio",
				"hello@example.com",
				null));
		User user = userRepository.saveAndFlush(new User(
				tenant,
				"staff@example.com",
				passwordEncoder.encode("password123"),
				role));
		return jwtTokenService.createAccessToken(AuthenticatedUser.from(user));
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
