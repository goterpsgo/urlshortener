package com.example.urlshortener.link;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
			UriComponentsBuilder uriBuilder) {
		Link link = linkService.createLink(request.originalUrl());
		String shortUrl = uriBuilder.replacePath("/{shortCode}").buildAndExpand(link.getShortCode()).toUriString();
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(new LinkResponse(link.getShortCode(), shortUrl, link.getOriginalUrl()));
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		return linkService.findByShortCode(shortCode)
			.map(link -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(link.getOriginalUrl())).<Void>build())
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

}
