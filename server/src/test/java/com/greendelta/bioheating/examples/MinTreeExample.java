package com.greendelta.bioheating.examples;

import java.awt.Color;
import java.io.File;

import javax.imageio.ImageIO;

import org.locationtech.jts.geom.Envelope;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.calc.graph.MinTree;
import com.greendelta.bioheating.io.GeoImage;
import com.greendelta.bioheating.model.Project;

public class MinTreeExample {

	public static void main(String[] args) {
		try (var db = Tests.db()) {
			System.out.println("Load project ...");
			var project = db.getAll(Project.class).getFirst();
			var minTree = MinTree.of(project).orElseThrow();

			// Calculate envelope for the image
			var envelope = new Envelope();
			for (var street : project.map().streets()) {
				for (var c : street.coordinates()) {
					envelope.expandToInclude(c);
				}
			}

			System.out.println("Drawing image ...");
			try (var img = new GeoImage(1024, 800, envelope)) {
				// Draw streets
				for (var street : project.map().streets()) {
					img.drawLine(street.coordinates(), Color.DARK_GRAY);
				}

				// Draw buildings
				var buildingColor = new Color(216, 27, 96);
				for (var building : project.map().buildings()) {
					img.drawPolygon(building.coordinates(), buildingColor);
				}

				// Draw minimal spanning tree edges in red
				for (var edge : minTree.getEdges()) {
					img.draw(edge.line(), Color.RED);
				}

				ImageIO.write(img.getImage(), "png", new File("target/graph-with-mst.png"));
				System.out.println("  .. saved image to target/graph-with-mst.png");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
