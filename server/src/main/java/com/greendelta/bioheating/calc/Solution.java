package com.greendelta.bioheating.calc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.locationtech.jts.geom.Envelope;

import com.greendelta.bioheating.model.GeoMap;

public record Solution(
	List<BuildingPolygon> buildings,
	List<StreetLine> streets,
	List<Connector> connectors
) {

	public static Solution empty() {
		return new Solution(
			List.of(), List.of(), List.of());
	}

	public static Solution calculate(GeoMap map) {
		if (map == null
			|| map.buildings().isEmpty()
			|| map.streets().isEmpty())
			return empty();

		// create building polygons
		var fun = GeometryBuilder.of(map);
		var bps = new ArrayList<BuildingPolygon>(map.buildings().size());
		for (var b : map.buildings()) {
			// TODO: only include buildings with respective state
			var res = fun.polygonOf(b);
			if (!res.hasError()) {
				// TODO: log errors
				bps.add(res.value());
			}
		}

		// create street lines
		var sls = new ArrayList<StreetLine>(map.streets().size());
		for (var s : map.streets()) {
			// TODO: only include streets with respective state
			var res = fun.lineOf(s);
			if (!res.hasError()) {
				sls.add(res.value());
			}
		}

		// create connectors
		var cons = new ArrayList<Connector>(bps.size() * sls.size());
		var buff = new ArrayList<Connector>(sls.size());
		for (var bp : bps) {
			for (var sl : sls) {
				var con = fun.connectorOf(bp, sl);
				if (!con.hasError()) {
					buff.add(con.value());
				}
			}
			buff.sort(Comparator.comparingDouble(Connector::length));
			if (buff.size() > 3) {
				cons.addAll(buff.subList(0, 3));
			} else {
				cons.addAll(buff);
			}
			buff.clear();
		}

		return new Solution(bps, sls, cons);
	}

	public boolean isEmpty() {
		return buildings.isEmpty() || streets.isEmpty();
	}

	public Envelope getEnvelope() {

		Envelope env = null;
		for (var b : buildings()) {
			var ei = b.polygon().getEnvelopeInternal();
			if (env == null) {
				env = ei;
			} else {
				env.expandToInclude(ei);
			}
		}

		for (var s : streets()) {
			var ei = s.line().getEnvelopeInternal();
			if (env == null) {
				env = ei;
			} else {
				env.expandToInclude(ei);
			}
		}

		return env != null
			? env
			: new Envelope(0, 0, 0, 0);
	}
}
