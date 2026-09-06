package com.bookflow.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.LoginRequest;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.security.AuthenticatedUser;
import com.bookflow.backend.security.JwtTokenService;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.Role;
import com.bookflow.backend.user.User;
import com.bookflow.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private TenantRepository tenantRepository;

	@Mock
	private UserRepository userRepository;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				authenticationManager,
				passwordEncoder,
				jwtTokenService,
				tenantRepository,
				userRepository);
	}

	@Test
	void registerCreatesNormalizedTenantAndOwnerAndReturnsToken() {
		RegisterRequest request = new RegisterRequest(
				"  Glow Studio  ",
				"  HELLO@EXAMPLE.COM  ",
				"  +353123456  ",
				"  OWNER@EXAMPLE.COM  ",
				"password123");
		when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
		when(tenantRepository.save(any(Tenant.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.saveAndFlush(any(User.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtTokenService.createAccessToken(any(AuthenticatedUser.class)))
				.thenReturn("access-token");

		AuthResponse response = authService.register(request);

		assertEquals("access-token", response.accessToken());
		assertEquals("owner@example.com", response.user().email());
		assertEquals(Role.OWNER, response.user().role());

		ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(tenantCaptor.capture());
		assertEquals("Glow Studio", tenantCaptor.getValue().getName());
		assertEquals("hello@example.com", tenantCaptor.getValue().getEmail());
		assertEquals("+353123456", tenantCaptor.getValue().getPhone());

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).saveAndFlush(userCaptor.capture());
		assertEquals(tenantCaptor.getValue(), userCaptor.getValue().getTenant());
		assertEquals("owner@example.com", userCaptor.getValue().getEmail());
		assertEquals("encoded-password", userCaptor.getValue().getPasswordHash());
		assertEquals(Role.OWNER, userCaptor.getValue().getRole());
	}

	@Test
	void registerConvertsBlankOptionalPhoneToNull() {
		RegisterRequest request = new RegisterRequest(
				"Glow Studio",
				"hello@example.com",
				"  ",
				"owner@example.com",
				"password123");
		when(tenantRepository.save(any(Tenant.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.saveAndFlush(any(User.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtTokenService.createAccessToken(any(AuthenticatedUser.class)))
				.thenReturn("access-token");

		authService.register(request);

		ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(tenantCaptor.capture());
		assertNull(tenantCaptor.getValue().getPhone());
	}

	@Test
	void registerRejectsAnExistingEmailBeforeCreatingTenant() {
		RegisterRequest request = new RegisterRequest(
				"Glow Studio",
				"hello@example.com",
				null,
				"OWNER@EXAMPLE.COM",
				"password123");
		when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> authService.register(request));

		verifyNoInteractions(tenantRepository, passwordEncoder, jwtTokenService);
		verify(userRepository, never()).saveAndFlush(any(User.class));
	}

	@Test
	void registerConvertsConcurrentEmailConstraintViolationToDuplicateResource() {
		RegisterRequest request = new RegisterRequest(
				"Glow Studio",
				"hello@example.com",
				null,
				"owner@example.com",
				"password123");
		when(tenantRepository.save(any(Tenant.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.saveAndFlush(any(User.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate email"));

		assertThrows(DuplicateResourceException.class, () -> authService.register(request));

		verifyNoInteractions(jwtTokenService);
	}

	@Test
	void loginAuthenticatesNormalizedEmailAndReturnsToken() {
		LoginRequest request = new LoginRequest("  OWNER@EXAMPLE.COM  ", "password123");
		AuthenticatedUser user = new AuthenticatedUser(
				1L,
				10L,
				"owner@example.com",
				"encoded-password",
				Role.OWNER,
				true);
		Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(user);
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenReturn(authentication);
		when(jwtTokenService.createAccessToken(user)).thenReturn("access-token");

		AuthResponse response = authService.login(request);

		assertEquals("access-token", response.accessToken());
		assertEquals("owner@example.com", response.user().email());
		assertEquals(Role.OWNER, response.user().role());

		ArgumentCaptor<Authentication> authenticationCaptor =
				ArgumentCaptor.forClass(Authentication.class);
		verify(authenticationManager).authenticate(authenticationCaptor.capture());
		UsernamePasswordAuthenticationToken token =
				(UsernamePasswordAuthenticationToken) authenticationCaptor.getValue();
		assertEquals("owner@example.com", token.getPrincipal());
		assertEquals("password123", token.getCredentials());
	}

	@Test
	void loginRejectsAnUnexpectedAuthenticationPrincipal() {
		LoginRequest request = new LoginRequest("owner@example.com", "password123");
		Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn("owner@example.com");
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenReturn(authentication);

		assertThrows(BadCredentialsException.class, () -> authService.login(request));

		verifyNoInteractions(jwtTokenService);
	}

	@Test
	void loginPropagatesInvalidCredentials() {
		LoginRequest request = new LoginRequest("owner@example.com", "wrong-password");
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenThrow(new BadCredentialsException("Invalid credentials"));

		assertThrows(BadCredentialsException.class, () -> authService.login(request));

		verifyNoInteractions(jwtTokenService);
	}
}
