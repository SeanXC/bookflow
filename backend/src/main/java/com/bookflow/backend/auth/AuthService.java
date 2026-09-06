package com.bookflow.backend.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.AuthenticatedUserResponse;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final TenantRepository tenantRepository;
	private final UserRepository userRepository;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String ownerEmail = normalizeEmail(request.ownerEmail());
		if (userRepository.existsByEmailIgnoreCase(ownerEmail)) {
			throw new DuplicateResourceException("A user with this email already exists");
		}

		try {
			Tenant tenant = tenantRepository.save(new Tenant(
					request.businessName().trim(),
					normalizeEmail(request.businessEmail()),
					normalizeOptional(request.businessPhone())));
			User user = userRepository.saveAndFlush(new User(
					tenant,
					ownerEmail,
					passwordEncoder.encode(request.password()),
					Role.OWNER));

			return createResponse(AuthenticatedUser.from(user));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateResourceException("A user with this email already exists");
		}
	}

	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(
						normalizeEmail(request.email()),
						request.password()));

		if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			throw new BadCredentialsException("Invalid credentials");
		}

		return createResponse(user);
	}

	private AuthResponse createResponse(AuthenticatedUser user) {
		return new AuthResponse(
				jwtTokenService.createAccessToken(user),
				AuthenticatedUserResponse.from(user));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
