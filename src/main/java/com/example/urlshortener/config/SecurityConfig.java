package com.example.urlshortener.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Value("${spring.h2.console.enabled:false}")
	private boolean h2ConsoleEnabled;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> {
			auth.requestMatchers(HttpMethod.GET, "/{shortCode}").permitAll();
			if (h2ConsoleEnabled) {
				auth.requestMatchers(PathRequest.toH2Console()).permitAll();
			}
			auth.anyRequest().authenticated();
		});

		http.csrf(csrf -> {
			csrf.ignoringRequestMatchers("/api/**");
			if (h2ConsoleEnabled) {
				csrf.ignoringRequestMatchers(PathRequest.toH2Console());
			}
		});

		if (h2ConsoleEnabled) {
			http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
		}

		http.httpBasic(withDefaults());
		http.formLogin(withDefaults());

		return http.build();
	}

}
