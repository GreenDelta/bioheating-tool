package com.greendelta.bioheating.config;

import java.io.IOException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthFilter extends UsernamePasswordAuthenticationFilter {

	private final AuthenticationManager authMan;
	private final ObjectMapper json;

	public AuthFilter(AuthenticationManager authMan, ObjectMapper json) {
		this.authMan = authMan;
		this.json = json;
		setFilterProcessesUrl("/api/users/login");
		setAuthenticationSuccessHandler(new SuccessHandler());
	}

	@Override
	public Authentication attemptAuthentication(
		HttpServletRequest req, HttpServletResponse resp
	) throws AuthenticationException {
		try {
			var authData = json.readValue(
				req.getInputStream(), AuthData.class);
			return authMan.authenticate(authData.token());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void successfulAuthentication(
		HttpServletRequest req,
		HttpServletResponse resp,
		FilterChain chain,
		Authentication authResult
	) throws IOException, ServletException {
		super.successfulAuthentication(req, resp, chain, authResult);
	}

	public record AuthData(String user, String password) {

		UsernamePasswordAuthenticationToken token() {
			return new UsernamePasswordAuthenticationToken(user, password);
		}
	}

	public static class SuccessHandler implements AuthenticationSuccessHandler {
		@Override
		public void onAuthenticationSuccess(
			HttpServletRequest req,
			HttpServletResponse resp,
			Authentication auth
		) throws IOException {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().write("Login successful");
			resp.getWriter().flush();
		}
	}
}

