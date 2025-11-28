package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.io.citygml.OsmBounds;
import com.greendelta.bioheating.io.citygml.OsmClient;

public class OsmClientExample {

	public static void main(String[] args) {
		try (var client = OsmClient.getDefault()) {
			var bounds = new OsmBounds(
				48.82975561604209, // south
				11.486790292463866, // west
				48.832196854541195, // north
				11.491462553643487); // east
			var streets = client.queryStreets(bounds);
			if (streets.isError()) {
				System.out.println("ERROR: " + streets.error());
				return;
			}
			for (var street : streets.value()) {
				var name = street.tags().get("name");
				if (name != null) {
					System.out.println(name);
				} else {
					System.out.println("unnamed street: " + street.id());
				}
				for (var tag : street.tags().entrySet()) {
					System.out.printf("  %s - %s%n", tag.getKey(), tag.getValue());
				}
			}
		}
	}
}
