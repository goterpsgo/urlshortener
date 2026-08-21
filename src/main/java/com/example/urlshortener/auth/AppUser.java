package com.example.urlshortener.auth;

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
@Table(name = "users")
@Getter
@NoArgsConstructor
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String username;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false, length = 20)
	private String role;

	@Column(name = "h1_green_enabled")
	private Boolean h1GreenEnabled;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public AppUser(String username, String passwordHash, String role) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public void setH1GreenEnabled(Boolean h1GreenEnabled) {
		this.h1GreenEnabled = h1GreenEnabled;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

}
