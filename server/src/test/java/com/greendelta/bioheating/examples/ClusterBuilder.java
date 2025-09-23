package com.greendelta.bioheating.examples;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.calc.search.BuildingShape;
import com.greendelta.bioheating.calc.search.StreetSegment;
import com.greendelta.bioheating.model.Inclusion;
import com.greendelta.bioheating.model.Project;

public class ClusterBuilder {

	private final Project project;
	private final GeometryFactory factory;

	public ClusterBuilder(Project project) {
		this.project = project;
		this.factory = new GeometryFactory();
	}

	public void build() {

		System.out.println("Create street segments");
		var segments = new ArrayList<StreetSegment>();
		for (var street : project.map().streets()) {
			segments.addAll(StreetSegment.allOf(street, factory));
		}
		System.out.println("  .. created " + segments.size() + " segments");

		System.out.println("Create building nodes");

		var nodes = project.map().buildings().stream()
			.filter(b -> b.isHeated() && b.inclusion() == Inclusion.REQUIRED)
			.map(b -> BuildingShape.of(b, factory))
			.toList();
		if (nodes.size() < 2)
			return;
		System.out.println("  .. created " + nodes.size() + " nodes");

		// TODO add the supply center, currently we just select one building
		var rand = ThreadLocalRandom.current();
		int supplyIdx = rand.nextInt(0, nodes.size());
		var root = nodes.get(supplyIdx);
		var env = root.center().getEnvelope();

		var handled = new TreeSet<Long>();
		handled.add(root.id());
		while (handled.size() < nodes.size()) {

			BuildingShape next = null;
			for (var node : nodes) {
				if (handled.contains(node.id())) {
					continue;
				}

			}

		}
	}



	private record Edge(
		LineString line
	) {

	}



	public static void main(String[] args) {
		try (var db = Tests.db()) {
			var project = db.getAll(Project.class).getFirst();
			new ClusterBuilder(project).build();
		}
	}

}
