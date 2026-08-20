package com.example.urlshortener.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

	@GetMapping({ "/app", "/app/{path:[^.]*}", "/app/**/{path:[^.]*}" })
	public String forwardToIndex() {
		return "forward:/app/index.html";
	}

}
