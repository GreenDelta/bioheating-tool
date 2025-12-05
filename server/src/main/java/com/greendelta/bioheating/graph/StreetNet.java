package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.index.strtree.STRtree;
import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.Node.StreetNode;
import com.greendelta.bioheating.model.Inclusion;

record StreetNet(List<Edge> edges, STRtree index) {

	static Res<StreetNet> create(GraphConfig config) {
		try {
			var net = new Builder(config).build();
			return Res.ok(net);
		} catch (Exception e) {
			return Res.error("Failed to connect the streets to a graph", e);
		}
	}

	private static class Builder {

		private final GraphConfig config;
		private final List<Edge> edges = new ArrayList<>();
		private final Map<Long, StreetNode> nodes = new HashMap<>();
		private final STRtree index = new STRtree();

		private Builder(GraphConfig config) {
			this.config = config;
		}

		private StreetNet build() {
			createEdges();
			index.build();
			return new StreetNet(edges, index);
		}

		private void createEdges() {
			for (var s : config.streets()) {
				if (s.inclusion() == Inclusion.EXCLUDED)
					continue;

				var cs = s.coordinates();
				if (cs == null || cs.length < 2)
					continue;
				for (int i = 1; i < cs.length; i++) {
					var start = cs[i - 1];
					var end = cs[i];

					var source = nodeOf(start);
					var target = nodeOf(end);
					if (source.equals(target))
						continue;

					var line = config.lineOf(start, end);
					var edge = config.edgeOf(source, target, line);
					edges.add(edge);
					index.insert(edge.envelope(), edge);
				}
			}
		}

		private StreetNode nodeOf(Coordinate coo) {
			long x = Math.round(coo.x);
			long y = Math.round(coo.y);

			// check if there is an existing street node within a 1-meter distance
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					var existing = nodes.get(gridKey(x + dx, y + dy));
					if (existing != null && config.isClose(coo, existing))
						return existing;
				}
			}

			var point = config.pointOf(coo);
			var newNode = new StreetNode(config.nextId(), point);
			nodes.put(gridKey(x, y), newNode);
			return newNode;
		}

		/// An UTM coordinate rounded to full metres can be stored without data loss
		/// in two 32-bit segments in a 64-bit number.
		private long gridKey(long x, long y) {
			return (x << 32) | (y & 0xFFFFFFFFL);
		}
	}
}
