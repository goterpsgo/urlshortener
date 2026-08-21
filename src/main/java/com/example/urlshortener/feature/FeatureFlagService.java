package com.example.urlshortener.feature;

import com.example.urlshortener.auth.AppUser;
import com.example.urlshortener.auth.AppUserRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

	private final AppUserRepository appUserRepository;
	private final boolean h1GreenDefault;

	public FeatureFlagService(AppUserRepository appUserRepository,
			@Value("${app.features.h1-green}") boolean h1GreenDefault) {
		this.appUserRepository = appUserRepository;
		this.h1GreenDefault = h1GreenDefault;
	}

	public boolean isH1GreenEnabled(String username) {
		return resolve(getH1GreenOverride(username));
	}

	public Boolean getH1GreenOverride(String username) {
		if (username == null) {
			return null;
		}
		return appUserRepository.findByUsername(username).map(AppUser::getH1GreenEnabled).orElse(null);
	}

	public void setH1GreenOverride(String username, Boolean override) {
		AppUser user = appUserRepository.findByUsername(username)
			.orElseThrow(() -> new IllegalStateException("No user found with username: " + username));
		user.setH1GreenEnabled(override);
		appUserRepository.save(user);
	}

	public Optional<AppUser> findUserById(Long userId) {
		return appUserRepository.findById(userId);
	}

	public Optional<AppUser> setH1GreenOverrideForUserId(Long userId, Boolean override) {
		return appUserRepository.findById(userId).map(user -> {
			user.setH1GreenEnabled(override);
			return appUserRepository.save(user);
		});
	}

	public boolean resolve(Boolean override) {
		return override != null ? override : h1GreenDefault;
	}

}
