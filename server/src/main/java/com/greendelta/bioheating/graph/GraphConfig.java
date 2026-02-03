package com.greendelta.bioheating.graph;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Street;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.openlca.commons.Res;

record GraphConfig(Project project, AtomicLong ids, GeometryFactory factory) {
	static Res<GraphConfig> of(Project project) {
		if (project == null || project.map() == null) return Res.error(
			"The provided project is empty."
		);
		if (project.map().streets().isEmpty()) return Res.error(
			"The project does not contain any streets"
		);

		boolean hasBuildings = false;
		long maxId = 0;
		for (var b : project.map().buildings()) {
			maxId = Math.max(maxId, b.id());
			if (isIncluded(b)) {
				hasBuildings = true;
			}
		}
		if (!hasBuildings) return Res.error(
			"Project does not contain any heated buildings"
		);

		var config = new GraphConfig(
			project,
			new AtomicLong(maxId),
			new GeometryFactory()
		);
		return Res.ok(config);
	}

	private static boolean isIncluded(Building b) {
		return (
			b != null && (b.isSupplyCenter() || (b.isHeated() && b.isIncluded()))
		);
	}

	List<Street> streets() {
		return project.map().streets();
	}

	long nextId() {
		return ids.incrementAndGet();
	}

	LineString lineOf(Coordinate... cs) {
		return factory.createLineString(cs);
	}

	Point pointOf(Coordinate coo) {
		return factory.createPoint(coo);
	}

	Edge edgeOf(Node source, Node target, LineString line) {
		return new Edge(nextId(), source, target, line, line.getLength());
	}

	boolean isClose(Coordinate coo, Node node) {
		return coo.distance(node.center().getCoordinate()) < 1;
	}

	void eachIncludedBuilding(Consumer<Building> fn) {
		for (var b : project.map().buildings()) {
			if (isIncluded(b)) {
				fn.accept(b);
			}
		}
	}
}
