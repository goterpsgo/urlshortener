package com.example.urlshortener.feature;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureFlagController {

	private final FeatureFlagService featureFlagService;

	public FeatureFlagController(FeatureFlagService featureFlagService) {
		this.featureFlagService = featureFlagService;
	}

	@GetMapping("/api/features")
	public FeatureFlagsResponse getFeatures(Authentication authentication) {
		return toResponse(resolveUsername(authentication));
	}

	@PutMapping("/api/features")
	public FeatureFlagsResponse updateFeatures(@RequestBody UpdateFeatureFlagsRequest request,
			Authentication authentication) {
		featureFlagService.setH1GreenOverride(authentication.getName(), request.h1Green());
		return toResponse(authentication.getName());
	}

	private String resolveUsername(Authentication authentication) {
		boolean isKnownUser = authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
		return isKnownUser ? authentication.getName() : null;
	}

	private FeatureFlagsResponse toResponse(String username) {
		return new FeatureFlagsResponse(featureFlagService.isH1GreenEnabled(username),
				featureFlagService.getH1GreenOverride(username));
	}

}
