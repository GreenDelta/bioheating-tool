package com.greendelta.bioheating.model.client;

import java.util.ArrayList;
import java.util.List;

import com.greendelta.bioheating.io.CoordinateTransformer;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.util.Res;

public record ClientMap(List<GeoFeature> features) {

	public static Res<ClientMap> of(GeoMap map) {
		var res = CoordinateTransformer.toWgs84From(map);
		if (res.hasError())
			return res.wrapError("failed to create transformer for map CRS");
		var wgs84 = res.value();

		var features = new ArrayList<GeoFeature>(
			map.buildings().size() + map.streets().size());
		for (var b : map.buildings()) {
			var f = GeoFeature.of(b, wgs84);
			if (!f.hasError()) {
				features.add(f.value());
			}
		}

		for (var s : map.streets()) {
			var f = GeoFeature.of(s, wgs84);
			if (!f.hasError()) {
				features.add(f.value());
			}
		}

		return Res.of(new ClientMap(features));
	}

}
