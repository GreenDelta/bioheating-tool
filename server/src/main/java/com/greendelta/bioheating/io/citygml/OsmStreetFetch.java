package com.greendelta.bioheating.io.citygml;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.openlca.commons.Res;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Street;

class OsmStreetFetch {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final GeoMap map;

	private OsmStreetFetch(GeoMap map) {
		this.map = map;
	}

	public static Res<Void> into(GeoMap map) {
		return map == null || map.buildings().isEmpty()
			? Res.error("no buildings found")
			: new OsmStreetFetch(map).doIt();
	}

	private Res<Void> doIt() {
		log.info("Fetching streets from OSM");

		// initialize the projector
		var transRes = CoordinateTransformer.fromWgs84To(map.crs());
		if (transRes.isError()) return transRes.wrapError(
			"failed to load projector"
		);
		var trans = transRes.value();

		// calculate the map bounds
		var bounds = OsmBounds.of(map);
		if (bounds.isError()) return bounds.wrapError("Failed to get map bounds.");

		// fetch the streets within these bounds
		var osm = fetchStreets(bounds.value());
		if (osm.isError()) return osm.wrapError("Failed to fetch OSM streets");
		var streets = osm.value();

		// convert the streets
		log.info("Fetched {} streets; convert them", streets.size());
		for (var s : streets) {
			var geometry = s.geometry();
			if (geometry == null || geometry.isEmpty()) continue;
			var street = convert(s, trans);
			if (street.isError()) {
				log.debug("Error in street conversion: {}", street.error());
				continue;
			}
			map.streets().add(street.value());
		}
		log.info("Added {} streets to map", map.streets().size());

		return Res.ok();
	}

	/// Fetch streets from OSM. Trys several times if it failes (the OSM servers
	/// block requests, when there are too many of them at the same time).
	private Res<List<OsmStreet>> fetchStreets(OsmBounds bounds) {
		try (var client = OsmClient.getDefault()) {
			int trial = 0;
			do {
				if (trial > 0) {
					log.warn(
						"Fetching OSM streets in trial {} failed; " +
							"waiting for next trial",
						trial
					);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return Res.error("Waiting for OSM fetch was interrupted", e);
					}
				}

				trial++;
				log.info("Fetch OSM streets for bounds={}; trial={}", bounds, trial);
				var res = client.queryStreets(bounds);
				if (res.isOk()) return res;
			} while (trial < 10);
			return Res.error("Failed to fetch OSM streets after several trials");
		} catch (Exception e) {
			return Res.error("Failed to fetch OSM streets", e);
		}
	}

	private Res<Street> convert(OsmStreet s, CoordinateTransformer trans) {
		if (
			s == null || s.geometry() == null || s.geometry().size() < 2
		) return Res.error("not a street with valid geometry");
		var geometry = s.geometry();
		var cs = new Coordinate[geometry.size()];
		for (int i = 0; i < geometry.size(); i++) {
			var osmCoord = geometry.get(i);
			cs[i] = new Coordinate(osmCoord.lon(), osmCoord.lat());
		}
		var transformed = trans.transform(cs);
		if (transformed.isError()) return transformed.castError();

		var name = s.tags() != null ? s.tags().get("name") : null;
		if (name == null) {
			name = "Unnamed path " + s.id();
		}

		var street = new Street()
			.name(name)
			.isExcluded(false)
			.coordinates(transformed.value());
		return Res.ok(street);
	}
}
