package com.example.urlshortener.link;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateLinkRequest(@NotBlank @URL String originalUrl) {

}
