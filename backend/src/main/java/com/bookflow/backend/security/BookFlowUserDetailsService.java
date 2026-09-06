package com.bookflow.backend.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookFlowUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public AuthenticatedUser loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepository.findByEmailIgnoreCase(email)
				.map(AuthenticatedUser::from)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
	}
}
