package com.greendelta.bioheating.graph;

import java.util.HashMap;
import java.util.function.Function;

import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.Node.BuildingNode;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;
import com.greendelta.bioheating.pipes.PipePlanImage;

public record MinTreeSolution(Project project, SpanningTree<Edge> tree) {

	public Res<Solution> create(Database db) {

		var solution = new Solution()
			.project(project);

		var nodes = new HashMap<Long, SolutionNode>();
		Function<Node, SolutionNode> nodeFetch = n -> {
			var node = nodes.get(n.id());
			if (node != null)
				return node;
			node = new SolutionNode()
				.x(n.center().getX())
				.y(n.center().getY());
			if (n instanceof BuildingNode bn) {
				node.building(bn.building());
			}
			nodes.put(n.id(), node);
			solution.nodes().add(node);
			return node;
		};

		for (var e : tree.getEdges()) {
			var source = nodeFetch.apply(e.source());
			var target = nodeFetch.apply(e.target());
			var edge = new SolutionEdge()
				.source(source)
				.target(target)
				.length(e.length())
				.coordinates(e.line().getCoordinates());
			solution.edges().add(edge);
		}

		// store the solution to create the IDs of nodes and edges
		db.insert(solution);

		// create the pipe plan image
		var imageRes = PipePlanImage.create(solution);
		if (imageRes.isError()) {
			db.delete(solution);
			return imageRes.wrapError("Failed to create pipe plan image");
		}

		// Update solution with the image
		solution.image(imageRes.value());
		deleteOutdatedOf(db, solution);
		solution.calculatedAt(System.currentTimeMillis());
		db.update(solution);

		return Res.ok(solution);
	}

	private void deleteOutdatedOf(Database db, Solution solution) {
		if (solution == null || solution.project() == null)
			return;
		for (var s : db.getAll(Solution.class)) {
			if (solution.equals(s)
				|| !solution.project().equals(s.project()))
				continue;
			db.delete(s);
		}
	}
}
