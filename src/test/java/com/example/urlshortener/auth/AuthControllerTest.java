package com.example.urlshortener.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthControllerTest {

	private final AuthController controller = new AuthController(mock(AppUserRepository.class), mock(PasswordEncoder.class),
			mock(AuthenticationManager.class), mock(JwtService.class));

	@Test
	void meReportsAdminForRoleAdminAuthority() {
		Authentication authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn("admin");
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

		MeResponse response = controller.me(authentication);

		assertThat(response.username()).isEqualTo("admin");
		assertThat(response.isAdmin()).isTrue();
	}

	@Test
	void meReportsNonAdminForRoleUserAuthority() {
		Authentication authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn("alice");
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();

		MeResponse response = controller.me(authentication);

		assertThat(response.username()).isEqualTo("alice");
		assertThat(response.isAdmin()).isFalse();
	}

}
