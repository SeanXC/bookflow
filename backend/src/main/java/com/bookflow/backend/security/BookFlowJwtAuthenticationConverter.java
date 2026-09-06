package com.bookflow.backend.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookFlowJwtAuthenticationConverter
		implements Converter<Jwt, AbstractAuthenticationToken> {

	private final BookFlowUserDetailsService userDetailsService;

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		String email = jwt.getSubject();
		if (email == null || email.isBlank()) {
			throw new BadCredentialsException("Invalid token subject");
		}

		AuthenticatedUser user = userDetailsService.loadUserByUsername(email);
		if (!user.isEnabled()) {
			throw new DisabledException("User account is disabled");
		}

		return UsernamePasswordAuthenticationToken.authenticated(
				user,
				null,
				user.getAuthorities());
	}
}
