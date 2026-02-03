package com.greendelta.bioheating.io.citygml;

import java.util.List;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.openlca.commons.Res;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NeighborAnalysis {

	private final double treshold = 0.15;
	private Logger log = LoggerFactory.getLogger(getClass());
	private final List<BuildingShape> shapes;

	private NeighborAnalysis(List<BuildingShape> shapes) {
		this.shapes = shapes;
	}

	static Res<Void> run(List<BuildingShape> shapes) {
		return new NeighborAnalysis(shapes).exec();
	}

	private Res<Void> exec() {
		if (shapes == null) return Res.error("No buildings found in CityGML model");
		log.info("run neighbor analysis of {} buildings", shapes.size());
		try {
			var index = buildIndex();
			findNeighbors(index);
			return Res.ok();
		} catch (Exception e) {
			return Res.error("Neighbor analysis of buildings failed", e);
		}
	}

	private STRtree buildIndex() {
		log.info("index buildings with STR tree");
		var index = new STRtree();
		for (var d : shapes) {
			index.insert(d.envelope(), d);
		}
		index.build();
		return index;
	}

	private void findNeighbors(STRtree index) {
		log.info("find neighbors of buildings");
		for (var d : shapes) {
			var q = d.envelope().copy();
			q.expandBy(treshold);
			for (var candidate : index.query(q)) {
				if (d == candidate) continue;
				if (
					!(candidate instanceof BuildingShape other) ||
					d.neighbors().contains(other.id())
				) continue;
				double distance = new DistanceOp(
					d.gml().groundSurface(),
					other.gml().groundSurface()
				).distance();
				if (distance <= treshold) {
					d.neighbors().add(other.id());
					other.neighbors().add(d.id());
				}
			}
		}
	}
}
