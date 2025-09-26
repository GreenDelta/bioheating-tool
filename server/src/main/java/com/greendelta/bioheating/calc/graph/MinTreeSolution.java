package com.greendelta.bioheating.calc.graph;

import java.util.HashMap;
import java.util.function.Function;

import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;

import com.greendelta.bioheating.calc.graph.Node.BuildingNode;
import com.greendelta.bioheating.model.Project;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;
import com.greendelta.bioheating.util.Res;

public record MinTreeSolution(Project project, SpanningTree<Edge> tree) {

	public Res<Solution> create() {

		var image = SolutionImage.create(project, tree);
		if (image.hasError())
			return image.wrapError("failed to create solution image");

		var solution = new Solution()
			.project(project)
			.image(image.value());

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

		return Res.of(solution);
	}
}
