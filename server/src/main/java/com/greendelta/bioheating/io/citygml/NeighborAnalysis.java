package com.greendelta.bioheating.io.citygml;

import java.util.List;

import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NeighborAnalysis {

	private final double treshold = 0.15;
	private Logger log = LoggerFactory.getLogger(getClass());

	void run(List<BuildingShape> data) {
		if (data == null)
			return;
		log.info("run neighbor analysis of {} buildings", data.size());

		log.info("index buildings with STR tree");
		var index = new STRtree();
		for (var d : data) {
			if (d.isEmpty())
				continue;
			index.insert(d.envelope(), d);
		}

		log.info("find neighbors of buildings");
		for (var d : data) {
			if (d.isEmpty())
				continue;
			var q = d.envelope().copy();
			q.expandBy(treshold);
			for (var candidate : index.query(q)) {
				if (d == candidate)
					continue;
				if (!(candidate instanceof BuildingShape other)
					|| other.isEmpty()
					|| d.neighbors().contains(other.id()))
					continue;
				double distance = new DistanceOp(
					d.gml().groundSurface(), other.gml().groundSurface())
						.distance();
				if (distance <= treshold) {
					d.neighbors().add(other.id());
					other.neighbors().add(d.id());
				}
			}
		}
	}
}
