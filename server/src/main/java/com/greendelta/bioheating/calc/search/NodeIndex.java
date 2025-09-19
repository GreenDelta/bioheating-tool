package com.greendelta.bioheating.calc.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.distance.DistanceOp;

import com.greendelta.bioheating.model.Project;

public record NodeIndex(Map<Long, Node> nodes) {

	private static NodeIndex empty() {
		return new NodeIndex(new HashMap<>());
	}

	public boolean isEmpty() {
		return nodes == null || nodes.isEmpty();
	}

	public int size() {
		return nodes != null ? nodes.size() : 0;
	}

	public static NodeIndex of(List<Node> nodes) {
		if (nodes == null || nodes.isEmpty())
			return empty();
		var map = new HashMap<Long, Node>();
		for (var node : nodes) {
			map.put(node.id(), node);
		}
		return new NodeIndex(map);
	}

	public static NodeIndex of(Project project, GeometryFactory f) {
		if (project == null || project.map() == null || f == null)
			return empty();
		var map = new HashMap<Long, Node>();
		project.map().buildings().stream()
			.map(b -> Node.of(b, f))
			.forEach(n -> map.put(n.id(), n));
		return new NodeIndex(map);
	}

	public Optional<Node> findClosestOf(Geometry g) {
		if (g == null || isEmpty())
			return Optional.empty();
		Node selected = null;
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
