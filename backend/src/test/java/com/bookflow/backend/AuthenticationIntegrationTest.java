package com.bookflow.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.auth.AuthService;
import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.security.AuthenticatedUser;
import com.bookflow.backend.security.JwtTokenService;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.Role;
import com.bookflow.backend.user.User;
import com.bookflow.backend.user.UserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
	"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthenticationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

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

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void openApiDocumentationIsPubliclyAccessible() throws Exception {
		mockMvc.perform(get("/api/docs/openapi"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("BookFlow API"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
			.andExpect(jsonPath("$.paths['/api/appointments'].get.tags[0]")
					.value("Appointments"))
			.andExpect(jsonPath("$.paths['/api/appointments'].post.responses['409']")
					.exists());
	}

	@Test
	void registerCreatesAnOwnerAndReturnsAccessToken() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "businessName": "Glow Studio",
						  "businessEmail": "hello@example.com",
						  "businessPhone": "+353123456",
						  "ownerEmail": "owner@example.com",
						  "password": "password123"
						}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.user.email").value("owner@example.com"))
			.andExpect(jsonPath("$.user.role").value("OWNER"));
	}

	@Test
	void duplicateRegistrationReturnsConflict() throws Exception {
		registerOwner("duplicate@example.com");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "businessName": "Another Studio",
						  "businessEmail": "another@example.com",
						  "ownerEmail": "duplicate@example.com",
						  "password": "password123"
						}
						"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
	}

	@Test
	void loginAuthenticatesAgainstTheDatabaseAndReturnsAccessToken() throws Exception {
		registerOwner("login@example.com");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "LOGIN@EXAMPLE.COM",
						  "password": "password123"
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.user.email").value("login@example.com"))
			.andExpect(jsonPath("$.user.role").value("OWNER"));
	}

	@Test
	void protectedEndpointWithoutTokenReturnsJsonUnauthorized() throws Exception {
		mockMvc.perform(get("/api/customers"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void configuredFrontendOriginCanCompleteCorsPreflight() throws Exception {
		mockMvc.perform(options("/api/services")
				.header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(
						HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
						"Authorization, Content-Type"))
			.andExpect(status().isOk())
			.andExpect(header().string(
					HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
					"http://localhost:5173"));
	}

	@Test
	void unconfiguredOriginIsRejectedByCors() throws Exception {
		mockMvc.perform(options("/api/services")
				.header(HttpHeaders.ORIGIN, "https://untrusted.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(
					HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void staffCannotCreateServices() throws Exception {
		String token = createTokenForRole("staff@example.com", Role.STAFF);

		mockMvc.perform(post("/api/services")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Haircut",
						  "description": "Cut and style",
						  "price": 30.00,
						  "durationMinutes": 30
						}
						"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void ownerCanCreateServices() throws Exception {
		String token = registerOwner("service-owner@example.com").accessToken();

		mockMvc.perform(post("/api/services")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Haircut",
						  "description": "Cut and style",
						  "price": 30.00,
						  "durationMinutes": 30
						}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Haircut"));
	}

	@Test
	void ownerCanViewTenantDashboardSummary() throws Exception {
		String token = registerOwner("dashboard-owner@example.com").accessToken();

		mockMvc.perform(get("/api/dashboard/summary")
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.todayAppointments").value(0))
			.andExpect(jsonPath("$.monthlyRevenue").value(0))
			.andExpect(jsonPath("$.activeCustomers").value(0))
			.andExpect(jsonPath("$.cancellationRate").value(0.00))
			.andExpect(jsonPath("$.businessTimeZone").value("UTC"));
	}

	@Test
	void staffCannotViewDashboardSummary() throws Exception {
		String token = createTokenForRole("dashboard-staff@example.com", Role.STAFF);

		mockMvc.perform(get("/api/dashboard/summary")
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void tenantOwnerCannotReadAnotherTenantsCustomer() throws Exception {
		String tenantAToken = registerOwner("tenant-a-owner@example.com").accessToken();
		Tenant tenantB = tenantRepository.saveAndFlush(new Tenant(
				"Studio B",
				"studio-b@example.com",
				null));
		Customer tenantBCustomer = customerRepository.saveAndFlush(new Customer(
				tenantB,
				"Emma",
				"Smith",
				"emma@example.com",
				null,
				null));

		mockMvc.perform(get("/api/customers/{customerId}", tenantBCustomer.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(tenantAToken)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void disabledUsersCannotUsePreviouslyIssuedTokens() throws Exception {
		AuthResponse response = registerOwner("disabled@example.com");
		jdbcTemplate.update(
				"UPDATE users SET enabled = FALSE WHERE email = ?",
				"disabled@example.com");
		entityManager.clear();

		mockMvc.perform(get("/api/services")
				.header(HttpHeaders.AUTHORIZATION, bearer(response.accessToken())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	private AuthResponse registerOwner(String ownerEmail) {
		return authService.register(new RegisterRequest(
				"Glow Studio",
				"hello+" + ownerEmail,
				null,
				ownerEmail,
				"password123"));
	}

	private String createTokenForRole(String email, Role role) {
		Tenant tenant = tenantRepository.saveAndFlush(new Tenant(
				"Glow Studio",
				"hello+" + email,
				null));
		User user = userRepository.saveAndFlush(new User(
				tenant,
				email,
				passwordEncoder.encode("password123"),
				role));
		return jwtTokenService.createAccessToken(AuthenticatedUser.from(user));
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
