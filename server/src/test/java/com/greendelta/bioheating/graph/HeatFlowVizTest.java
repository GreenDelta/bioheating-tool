package com.greendelta.bioheating.graph;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Solution;
import com.greendelta.bioheating.model.SolutionEdge;
import com.greendelta.bioheating.model.SolutionNode;

public class HeatFlowVizTest {

	@Test
	public void testToDot() {
		Solution solution = new Solution();

		// Create supply center
		Building supplyBuilding = new Building()
			.name("Supply Center")
			.isSupplyCenter(true);
		supplyBuilding.id(1);
		SolutionNode rootNode = new SolutionNode()
			.building(supplyBuilding);
		rootNode.id(1);
		solution.nodes().add(rootNode);

		// Create a street node
		SolutionNode streetNode = new SolutionNode()
			.x(10.0)
			.y(20.0);
		streetNode.id(2);
		solution.nodes().add(streetNode);

		// Create a heated building
		Building heatedBuilding = new Building()
			.name("Heated Building")
			.isHeated(true);
		heatedBuilding.id(3);
		SolutionNode leafNode = new SolutionNode()
			.building(heatedBuilding);
		leafNode.id(3);
		solution.nodes().add(leafNode);

		// Create edges: root -> street -> leaf
		SolutionEdge edge1 = new SolutionEdge()
			.source(rootNode)
			.target(streetNode)
			.length(100.0);
		solution.edges().add(edge1);

		SolutionEdge edge2 = new SolutionEdge()
			.source(streetNode)
			.target(leafNode)
			.length(50.0);
		solution.edges().add(edge2);

		var treeRes = HeatFlowTree.of(solution);
		assertTrue(treeRes.isOk());
		var tree = treeRes.value();

		String dot = HeatFlowViz.toDot(tree);
		assertNotNull(dot);

		// Verify DOT contains all nodes and edges
		assertTrue(dot.contains("digraph CalcTree"));
		assertTrue(dot.contains("Supply Center"));
		assertTrue(dot.contains("street [x=10.0, y=20.0]"));
		assertTrue(dot.contains("Heated Building"));

		// Verify edges are correctly formatted (not -> null)
		// We use n1, n2, n3 because of seq.incrementAndGet()
		assertTrue(dot.contains("n1 -> n2"));
		assertTrue(dot.contains("n2 -> n3"));
	}
}
