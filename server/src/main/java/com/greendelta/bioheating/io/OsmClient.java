package com.greendelta.bioheating.io;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.locationtech.jts.geom.Coordinate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greendelta.bioheating.util.Res;
import com.greendelta.bioheating.util.Strings;

public class OsmClient implements AutoCloseable {

	private final String api;
	private final HttpClient http;

	private OsmClient(String api) {
		this.api = Objects.requireNonNull(api);
		this.http = HttpClient.newHttpClient();
	}

	public static OsmClient of(String api) {
		return Strings.isNil(api)
			? getDefault()
			: new OsmClient(api);
	}

	public static OsmClient getDefault() {
		return new OsmClient("https://overpass-api.de/api/interpreter");
	}

	public Res<List<OsmStreet>> queryStreets(
		Coordinate southWest, Coordinate northEast
	) {
		if (southWest == null || northEast == null)
			return Res.error("coordinates cannot be null");

		// Extract bounding box coordinates
		double south = southWest.y;
		double west = southWest.x;
		double north = northEast.y;
		double east = northEast.x;

		// Build the Overpass query
		String query = String.format(
			"[out:json];way[highway](%f,%f,%f,%f);out geom;",
			south, west, north, east);

		try {
			// Prepare the request
			String formData = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(api))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(formData))
				.build();

			// Execute the request
			HttpResponse<String> response = http.send(request,
				HttpResponse.BodyHandlers.ofString());

			// Process the response
			if (response.statusCode() != 200) {
				return Res.error("HTTP error: " + response.statusCode() + " - " + response.body());
			}

			// Parse the JSON response
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response.body());
			JsonNode elements = root.get("elements");

			if (elements == null || !elements.isArray()) {
				return Res.error("Invalid response format: elements array not found");
			}

			// Convert to OsmStreet objects
			List<OsmStreet> streets = new ArrayList<>();
			for (JsonNode element : elements) {
				if (element.isObject()) {
					streets.add(new OsmStreet(mapper.treeToValue(element, ObjectNode.class)));
				}
			}

			return Res.of(streets);

		} catch (IOException e) {
			return Res.error("IO error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Res.error("Request interrupted: " + e.getMessage(), e);
		} catch (Exception e) {
			return Res.error("Unexpected error: " + e.getMessage(), e);
		}
	}

	@Override
	public void close() throws Exception {
		http.close();
	}
}
