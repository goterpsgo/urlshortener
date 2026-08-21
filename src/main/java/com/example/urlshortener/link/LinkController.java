package com.example.urlshortener.link;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class LinkController {

	private final LinkService linkService;

	public LinkController(LinkService linkService) {
		this.linkService = linkService;
	}

	@PostMapping("/api/links")
	public ResponseEntity<LinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request,
			UriComponentsBuilder uriBuilder, Authentication authentication) {
		Link link = linkService.createLink(request.originalUrl(), authentication.getName());
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(link, uriBuilder));
	}

	@GetMapping("/api/links")
	public List<LinkResponse> listLinks(UriComponentsBuilder uriBuilder, Authentication authentication) {
		return linkService.listLinks(authentication.getName())
			.stream()
			.map(link -> toResponse(link, uriBuilder))
			.toList();
	}

	@PutMapping("/api/links/{id}")
	public ResponseEntity<LinkResponse> updateLink(@PathVariable Long id, @Valid @RequestBody UpdateLinkRequest request,
			UriComponentsBuilder uriBuilder, Authentication authentication) {
		return linkService.updateLink(id, authentication.getName(), request.originalUrl())
			.map(link -> ResponseEntity.ok(toResponse(link, uriBuilder)))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		return linkService.findByShortCode(shortCode)
			.map(link -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(link.getOriginalUrl())).<Void>build())
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private LinkResponse toResponse(Link link, UriComponentsBuilder uriBuilder) {
		String shortUrl = uriBuilder.replacePath("/{shortCode}").buildAndExpand(link.getShortCode()).toUriString();
		return new LinkResponse(link.getId(), link.getShortCode(), shortUrl, link.getOriginalUrl());
	}

}
