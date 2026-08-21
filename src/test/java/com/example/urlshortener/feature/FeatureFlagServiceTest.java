package com.example.urlshortener.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.urlshortener.auth.AppUser;
import com.example.urlshortener.auth.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FeatureFlagServiceTest {

	@Test
	void fallsBackToEnvironmentDefaultWhenNoOverride() {
		AppUserRepository repository = mock(AppUserRepository.class);
		AppUser user = new AppUser("alice", "hash", "USER");
		when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

		FeatureFlagService service = new FeatureFlagService(repository, true);

		assertThat(service.isH1GreenEnabled("alice")).isTrue();
		assertThat(service.getH1GreenOverride("alice")).isNull();
	}

	@Test
	void perUserOverrideWinsOverEnvironmentDefault() {
		AppUserRepository repository = mock(AppUserRepository.class);
		AppUser user = new AppUser("alice", "hash", "USER");
		user.setH1GreenEnabled(Boolean.TRUE);
		when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

		FeatureFlagService service = new FeatureFlagService(repository, false);

		assertThat(service.isH1GreenEnabled("alice")).isTrue();
		assertThat(service.getH1GreenOverride("alice")).isTrue();
	}

	@Test
	void unknownUserFallsBackToEnvironmentDefault() {
		AppUserRepository repository = mock(AppUserRepository.class);
		when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

		FeatureFlagService service = new FeatureFlagService(repository, true);

		assertThat(service.isH1GreenEnabled("ghost")).isTrue();
	}

	@Test
	void anonymousVisitorGetsEnvironmentDefault() {
		AppUserRepository repository = mock(AppUserRepository.class);

		FeatureFlagService service = new FeatureFlagService(repository, true);

		assertThat(service.isH1GreenEnabled(null)).isTrue();
		assertThat(service.getH1GreenOverride(null)).isNull();
	}

	@Test
	void setH1GreenOverridePersistsToTheUser() {
		AppUserRepository repository = mock(AppUserRepository.class);
		AppUser user = new AppUser("alice", "hash", "USER");
		when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

		FeatureFlagService service = new FeatureFlagService(repository, false);
		service.setH1GreenOverride("alice", Boolean.TRUE);

		assertThat(user.getH1GreenEnabled()).isTrue();
		verify(repository).save(user);
	}

	@Test
	void setH1GreenOverrideToNullClearsIt() {
		AppUserRepository repository = mock(AppUserRepository.class);
		AppUser user = new AppUser("alice", "hash", "USER");
		user.setH1GreenEnabled(Boolean.TRUE);
		when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

		FeatureFlagService service = new FeatureFlagService(repository, false);
		service.setH1GreenOverride("alice", null);

		assertThat(user.getH1GreenEnabled()).isNull();
		assertThat(service.isH1GreenEnabled("alice")).isFalse();
	}

	@Test
	void resolveFallsBackToDefaultWhenOverrideIsNull() {
		FeatureFlagService service = new FeatureFlagService(mock(AppUserRepository.class), true);

		assertThat(service.resolve(null)).isTrue();
		assertThat(service.resolve(Boolean.FALSE)).isFalse();
	}

	@Test
	void setH1GreenOverrideForUserIdPersistsAndReturnsTheUser() {
		AppUserRepository repository = mock(AppUserRepository.class);
		AppUser user = new AppUser("alice", "hash", "USER");
		when(repository.findById(42L)).thenReturn(Optional.of(user));
		when(repository.save(user)).thenReturn(user);

		FeatureFlagService service = new FeatureFlagService(repository, false);
		Optional<AppUser> result = service.setH1GreenOverrideForUserId(42L, Boolean.TRUE);

		assertThat(result).contains(user);
		assertThat(user.getH1GreenEnabled()).isTrue();
		verify(repository).save(user);
	}

	@Test
	void setH1GreenOverrideForUnknownUserIdReturnsEmpty() {
		AppUserRepository repository = mock(AppUserRepository.class);
		when(repository.findById(99L)).thenReturn(Optional.empty());

		FeatureFlagService service = new FeatureFlagService(repository, false);

		assertThat(service.setH1GreenOverrideForUserId(99L, Boolean.TRUE)).isEmpty();
	}

}
