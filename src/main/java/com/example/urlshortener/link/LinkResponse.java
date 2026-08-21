package com.example.urlshortener.link;

public record LinkResponse(Long id, String shortCode, String shortUrl, String originalUrl) {

}
