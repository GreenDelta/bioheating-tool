package com.greendelta.bioheating.calc.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.distance.DistanceOp;

import com.greendelta.bioheating.model.Project;

public record NodeIndex(Map<Long, BuildingShape> nodes) {

	private static NodeIndex empty() {
		return new NodeIndex(new HashMap<>());
	}

	public boolean isEmpty() {
		return nodes == null || nodes.isEmpty();
	}

	public int size() {
		return nodes != null ? nodes.size() : 0;
	}

	public static NodeIndex of(List<BuildingShape> nodes) {
		if (nodes == null || nodes.isEmpty())
			return empty();
		var map = new HashMap<Long, BuildingShape>();
		for (var node : nodes) {
			map.put(node.id(), node);
		}
		return new NodeIndex(map);
	}

	public static NodeIndex of(Project project, GeometryFactory f) {
		if (project == null || project.map() == null || f == null)
			return empty();
		var map = new HashMap<Long, BuildingShape>();
		project.map().buildings().stream()
			.map(b -> BuildingShape.of(b, f))
			.forEach(n -> map.put(n.id(), n));
		return new NodeIndex(map);
	}

	public Optional<BuildingShape> findClosestOf(Geometry g) {
		if (g == null || isEmpty())
			return Optional.empty();
		BuildingShape selected = null;
		double distance = Double.MAX_VALUE;
		for (var node : nodes.values()) {
			double dist = DistanceOp.distance(g, node.center());
			if (selected == null || distance > dist) {
				selected = node;
				distance = dist;
			}
		}
		return Optional.ofNullable(selected);
	}



}
