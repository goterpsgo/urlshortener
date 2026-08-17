package com.example.urlshortener.link;

import java.security.SecureRandom;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LinkService {

	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int CODE_LENGTH = 7;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final LinkRepository linkRepository;

	public LinkService(LinkRepository linkRepository) {
		this.linkRepository = linkRepository;
	}

	public Link createLink(String originalUrl) {
		String shortCode;
		do {
			shortCode = generateCode();
		} while (linkRepository.findByShortCode(shortCode).isPresent());

		return linkRepository.save(new Link(shortCode, originalUrl));
	}

	public Optional<Link> findByShortCode(String shortCode) {
		return linkRepository.findByShortCode(shortCode);
	}

	private String generateCode() {
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}

}
