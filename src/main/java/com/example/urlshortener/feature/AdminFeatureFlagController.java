package com.example.urlshortener.feature;

import com.example.urlshortener.auth.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminFeatureFlagController {

	private final FeatureFlagService featureFlagService;

	public AdminFeatureFlagController(FeatureFlagService featureFlagService) {
		this.featureFlagService = featureFlagService;
	}

	@GetMapping("/api/admin/users/{userId}/features")
	public ResponseEntity<AdminFeatureFlagsResponse> getFeaturesForUser(@PathVariable Long userId) {
		return featureFlagService.findUserById(userId)
			.map(this::toResponse)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PutMapping("/api/admin/users/{userId}/features")
	public ResponseEntity<AdminFeatureFlagsResponse> updateFeaturesForUser(@PathVariable Long userId,
			@RequestBody UpdateFeatureFlagsRequest request) {
		return featureFlagService.setH1GreenOverrideForUserId(userId, request.h1Green())
			.map(this::toResponse)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private AdminFeatureFlagsResponse toResponse(AppUser user) {
		Boolean override = user.getH1GreenEnabled();
		return new AdminFeatureFlagsResponse(user.getId(), user.getUsername(), featureFlagService.resolve(override),
				override);
	}

}
