package com.greendelta.bioheating.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import com.greendelta.bioheating.calc.Solution;
import com.greendelta.bioheating.util.Res;

public class SolutionImage {

	private final Solution solution;
	private final File file;
	private final int size;

	private final Envelope envelope;
	private final double scale;
	private final Color STREET_COLOR = new Color(120, 144, 156);
	private final Color BUILDING_COLOR = new Color(216, 27, 96);
	private final Color CONNECTOR_COLOR = new Color(142,36,170);

	private SolutionImage(
		Solution solution, File file, int size
	) {
		this.solution = solution;
		this.file = file;
		this.size = size;
		this.envelope = solution.getEnvelope();
		this.scale = (size - 10) /
			Math.max(envelope.getWidth(), envelope.getHeight());
	}

	public static Res<Void> write(Solution solution, File file) {
		return write(solution, file, 1024);
	}

	public static Res<Void> write(Solution solution, File file, int size) {
		if (solution == null || solution.isEmpty())
			return Res.error("solution is empty");
		if (file == null)
			return Res.error("no valid file provided");
		return new SolutionImage(solution, file, size).write();
	}

	private Res<Void> write() {
		try {
			var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
			var g2d = image.createGraphics();
			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, size, size);

			renderBuildings(g2d);
			renderStreets(g2d);
			renderConnectors(g2d);

			ImageIO.write(image, "png", file);
			g2d.dispose();
			return Res.VOID;
		} catch (Exception e) {
			return Res.error("failed to create solution image", e);
		}
	}

	private void renderBuildings(Graphics2D g2d) {
		for (var b : solution.buildings()) {
			var shape = shapeOf(b.polygon());
			shape.fillPolygon(g2d, BUILDING_COLOR);
			shape.drawPolygon(g2d, Color.black);
		}
	}

	private void renderStreets(Graphics2D g2d) {
		for (var s : solution.streets()) {
			var shape = shapeOf(s.line());
			shape.drawLine(g2d, STREET_COLOR);
		}
	}

	private void renderConnectors(Graphics2D g2d) {
		for (var c : solution.connectors()) {
			var shape = shapeOf(c.connectorLine());
			shape.drawLine(g2d, CONNECTOR_COLOR);
		}
	}

	private Shape shapeOf(Geometry g) {
		var minX = envelope.getMinX();
		var minY = envelope.getMinY();
		var cs = g.getCoordinates();
		int n = cs.length;
		var xs = new int[n];
		var ys = new int[n];
		for (int i = 0; i < n; i++) {
			var point = cs[i];
			xs[i] = 5 + (int) ((point.getX() - minX) * scale);
			ys[i] = 5 + (int) (size - ((point.getY() - minY) * scale));
		}
		return new Shape(xs, ys, n);
	}

	private record Shape(int[] xs, int[] ys, int n) {

		void fillPolygon(Graphics2D g2d, Color color) {
			g2d.setColor(color);
			g2d.fillPolygon(xs, ys, n);
		}

		void drawPolygon(Graphics2D g2d, Color color) {
			g2d.setColor(color);
			g2d.drawPolygon(xs, ys, n);
		}

		void drawLine(Graphics2D g2d, Color color) {
			g2d.setColor(color);
			for (int j = 1; j < n; j++) {
				int i = j - 1;
				g2d.drawLine(xs[i], ys[i], xs[j], ys[j]);
			}
		}
	}

}
