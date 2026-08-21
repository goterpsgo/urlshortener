package com.example.urlshortener.link;

import java.security.SecureRandom;
import java.util.List;
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

	public Link createLink(String originalUrl, String ownerUsername) {
		String shortCode;
		do {
			shortCode = generateCode();
		} while (linkRepository.findByShortCode(shortCode).isPresent());

		return linkRepository.save(new Link(shortCode, originalUrl, ownerUsername));
	}

	public Optional<Link> findByShortCode(String shortCode) {
		return linkRepository.findByShortCode(shortCode);
	}

	public List<Link> listLinks(String ownerUsername) {
		return linkRepository.findByOwnerUsernameOrderByCreatedAtDesc(ownerUsername);
	}

	public Optional<Link> updateLink(Long id, String ownerUsername, String originalUrl) {
		return linkRepository.findByIdAndOwnerUsername(id, ownerUsername).map(link -> {
			link.setOriginalUrl(originalUrl);
			return linkRepository.save(link);
		});
	}

	private String generateCode() {
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}

}
