package com.greendelta.bioheating.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class WebController {

	/**
	 * For requests that end with a slash, we serve the index.html file of
	 * the respective folder.
	 */
	@GetMapping({"/*/", "/*/*/", "/*/*/*/", "/*/*/*/*/", "/*/*/*/*/*/"})
	public RedirectView serveIndexFile(HttpServletRequest req) {
		var redirect = req.getRequestURI() + "index.html";
		var q = req.getQueryString();
		if (q != null) {
			redirect += "?" + q;
		}
		return new RedirectView(redirect);
	}
}

