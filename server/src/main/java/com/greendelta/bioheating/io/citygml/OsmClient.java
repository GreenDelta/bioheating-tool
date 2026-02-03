package com.greendelta.bioheating.io.citygml;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.openlca.commons.Res;
import org.openlca.commons.Strings;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OsmClient implements AutoCloseable {

	private final String api;
	private final HttpClient http;

	private OsmClient(String api) {
		this.api = Objects.requireNonNull(api);
		this.http = HttpClient.newHttpClient();
	}

	public static OsmClient of(String api) {
		return Strings.isBlank(api) ? getDefault() : new OsmClient(api);
	}

	public static OsmClient getDefault() {
		return new OsmClient("https://overpass-api.de/api/interpreter");
	}

	public Res<List<OsmStreet>> queryStreets(OsmBounds bounds) {
		try {
			var query = String.format(
				"[out:json];way[highway](%f,%f,%f,%f);out geom;",
				bounds.south(),
				bounds.west(),
				bounds.north(),
				bounds.east()
			);
			var formData = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
			var request = HttpRequest.newBuilder()
				.uri(URI.create(api))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(formData))
				.build();

			var response = http.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return Res.error(
					"HTTP error: " + response.statusCode() + " - " + response.body()
				);
			}

			var mapper = new ObjectMapper();
			var elements = mapper.readTree(response.body()).get("elements");
			return OsmStreet.allFrom(elements, mapper);
		} catch (Exception e) {
			return Res.error("fetching OSM streets failed", e);
		}
	}

	@Override
	public void close() {
		http.close();
	}
}
