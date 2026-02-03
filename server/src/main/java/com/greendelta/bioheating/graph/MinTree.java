package com.greendelta.bioheating.graph;

import com.greendelta.bioheating.graph.Node.BuildingNode;
import com.greendelta.bioheating.model.Project;
import java.util.concurrent.ThreadLocalRandom;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm.SpanningTree;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.openlca.commons.Res;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinTree {

	private MinTree() {}

	public static Res<SpanningTree<Edge>> of(Project project) {
		return project != null
			? new Builder(project).build()
			: Res.error("no project provided");
	}

	private static class Builder {

		private final Logger log = LoggerFactory.getLogger(getClass());
		private final Project project;

		Builder(Project project) {
			this.project = project;
		}

		Res<SpanningTree<Edge>> build() {
			log.info("create minimal spanning tree of project {}", project.id());
			try {
				log.info("build full graph");
				var graphRes = Graph.buildFrom(project);
				if (graphRes.isError()) return graphRes.wrapError(
					"failed to create project graph"
				);
				var g = graphRes.value();
				log.info(
					"created graph with {} nodes and {} edges",
					g.vertexSet().size(),
					g.edgeSet().size()
				);

				log.info("select center node");
				var buildingNodes = g
					.vertexSet()
					.stream()
					.filter(BuildingNode.class::isInstance)
					.map(BuildingNode.class::cast)
					.toList();
				var cix = ThreadLocalRandom.current().nextInt(buildingNodes.size());
				var center = buildingNodes.get(cix);
				log.info("selected center: {}", center.id());

				log.info("build minimal graph");
				var minGraph = new Graph();
				for (var target : buildingNodes) {
					if (target.equals(center)) continue;
					var path = DijkstraShortestPath.findPathBetween(g, center, target);
					minGraph.add(path);
				}
				log.info(
					"created graph with {} nodes and {} edges",
					minGraph.vertexSet().size(),
					minGraph.edgeSet().size()
				);

				log.info("build minimal spanning tree");
				var tree = new KruskalMinimumSpanningTree<>(minGraph).getSpanningTree();
				log.info("created tree with {} edges", tree.getEdges().size());

				return Res.ok(tree);
			} catch (Exception e) {
				return Res.error("failed to build minimal spanning tree", e);
			}
		}
	}
}
