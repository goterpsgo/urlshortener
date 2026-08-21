package com.example.urlshortener.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {

	private static final String DEFAULT_ROLE = "USER";

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, JwtService jwtService) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@PostMapping("/auth/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		if (appUserRepository.existsByUsername(request.username())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
		}

		AppUser appUser = new AppUser(request.username(), passwordEncoder.encode(request.password()), DEFAULT_ROLE);
		appUserRepository.save(appUser);

		String token = jwtService.generateToken(appUser.getUsername(), appUser.getRole());
		return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token));
	}

	@PostMapping("/auth/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		try {
			authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		}
		catch (AuthenticationException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}

		AppUser appUser = appUserRepository.findByUsername(request.username()).orElseThrow();
		String token = jwtService.generateToken(appUser.getUsername(), appUser.getRole());
		return ResponseEntity.ok(new AuthResponse(token));
	}

	@GetMapping("/api/me")
	public MeResponse me(Authentication authentication) {
		boolean isAdmin = authentication.getAuthorities()
			.stream()
			.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
		return new MeResponse(authentication.getName(), isAdmin);
	}

}
