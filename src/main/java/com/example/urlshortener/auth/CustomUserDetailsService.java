package com.example.urlshortener.auth;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;

	public CustomUserDetailsService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser appUser = appUserRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("No user found with username: " + username));

		return org.springframework.security.core.userdetails.User.withUsername(appUser.getUsername())
			.password(appUser.getPasswordHash())
			.authorities(List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole())))
			.build();
	}

}
