package com.greendelta.bioheating.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class AuthConfig {

	private final ObjectMapper json;

	public AuthConfig(ObjectMapper json) {
		this.json = json;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http, AuthenticationConfiguration authConfig
	) throws Exception {

		var authMan = authConfig.getAuthenticationManager();
		var authFilter = new AuthFilter(authMan, json);
		authFilter.setFilterProcessesUrl("/api/users/login");

		http
			.cors(conf -> conf.configurationSource(cors()))
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.securityContext(ctx -> ctx.requireExplicitSave(false))

			//.rememberMe(conf -> conf.rememberMeServices(rememberMeServices).tokenValiditySeconds(60 * 60 * 24))

			.securityMatcher("/**").authorizeHttpRequests(authorize -> authorize

				.requestMatchers(
					"/api/users/login",
					"/api/users/logout")
				.permitAll()

				.requestMatchers(
					"/api/epds",
					"/api/epds/**",
					"/api/processes",
					"/api/processes/**"
				).authenticated()

				.requestMatchers(HttpMethod.GET, "/api/users")
				.hasRole("ADMIN")

				.requestMatchers(HttpMethod.POST,
					"/api/users",
					"/api/providers",
					"/api/units")
				.hasRole("ADMIN")

				.anyRequest().permitAll()
			)
			.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
			.logout(logout -> logout
				.logoutUrl("/api/users/logout")
				.addLogoutHandler(new SecurityContextLogoutHandler())
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
			);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	public CorsConfigurationSource cors() {
		var source = new UrlBasedCorsConfigurationSource();
		var config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.addAllowedOriginPattern("*"); // Allows all origins
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}
}

