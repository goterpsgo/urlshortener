package com.example.urlshortener.link;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "links")
@Getter
@NoArgsConstructor
public class Link {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "short_code", nullable = false, unique = true, length = 10)
	private String shortCode;

	@Column(name = "original_url", nullable = false, length = 2048)
	private String originalUrl;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Link(String shortCode, String originalUrl) {
		this.shortCode = shortCode;
		this.originalUrl = originalUrl;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

}
