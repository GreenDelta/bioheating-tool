package com.greendelta.bioheating.io;

import java.net.http.HttpClient;
import java.util.Objects;

import com.greendelta.bioheating.util.Strings;

public class OverpassClient implements AutoCloseable {

	private final String api;
	private final HttpClient http;

	private OverpassClient(String api) {
		this.api = Objects.requireNonNull(api);
		this.http = HttpClient.newHttpClient();
	}

	public static OverpassClient of(String api) {
		return Strings.isNil(api)
			? getDefault()
			: new OverpassClient(api);
	}

	public static OverpassClient getDefault() {
		return new OverpassClient("https://overpass-api.de/api/interpreter");
	}


	@Override
	public void close() throws Exception {
		http.close();
	}
}
