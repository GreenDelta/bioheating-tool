package com.greendelta.bioheating.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

	/// For all UI routes (used by React Router), serve the index.html file
	/// so that client-side routing can handle the request.
	@GetMapping("/ui/**")
	public String serveReactApp() {
		return "forward:/index.html";
	}
}

