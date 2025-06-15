package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.io.OsmClient;

public class OsmClientExample {

	public static void main(String[] args) {
		try (var client = OsmClient.getDefault()) {
			var streets = client.queryStreets(
				48.82975561604209, 11.486790292463866, 48.832196854541195, 11.491462553643487
			);
			if (streets.hasError()) {
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
			}
		}
	}
}
