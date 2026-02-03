package com.greendelta.bioheating.io;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

public class GeoImage implements AutoCloseable {

	private final int margin = 5;
	private final int height;
	private final double scaleX;
	private final double scaleY;
	private final double minX;
	private final double minY;
	private final BufferedImage image;
	private final Graphics2D g;

	public GeoImage(int size, Envelope envelope) {
		this(size, size, envelope);
	}

	public GeoImage(int width, int height, Envelope envelope) {
		this.height = height;
		this.scaleX = (width - 2 * margin) / envelope.getWidth();
		this.minX = envelope.getMinX();
		this.scaleY = (height - 2 * margin) / envelope.getHeight();
		this.minY = envelope.getMinY();
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		g = image.createGraphics();
		g.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON
		);
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, width, height);
	}

	public BufferedImage getImage() {
		return image;
	}

	public void draw(Polygon polygon, Color color) {
		if (polygon != null) {
			drawPolygon(polygon.getCoordinates(), color);
		}
	}

	public void draw(Polygon polygon, Color border, Color fill) {
		if (polygon != null) {
			drawPolygon(polygon.getCoordinates(), border, fill);
		}
	}

	public void draw(LineString line, Color color) {
		if (line != null) {
			drawLine(line.getCoordinates(), color);
		}
	}

	public void draw(LineString line, Color color, float strokeWidth) {
		if (line != null) {
			drawLine(line.getCoordinates(), color, strokeWidth);
		}
	}

	public void drawPolygon(Coordinate[] cs, Color color) {
		if (cs == null || color == null) return;
		var fill = new Color(
			color.getRed(),
			color.getGreen(),
			color.getBlue(),
			128
		);
		drawPolygon(cs, color, fill);
	}

	public void drawPolygon(Coordinate[] cs, Color border, Color fill) {
		var shape = shapeOf(cs);
		shape.fillPolygon(g, fill);
		shape.drawPolygon(g, border);
	}

	public void drawLine(Coordinate[] cs, Color color) {
		if (cs == null || color == null) return;
		var shape = shapeOf(cs);
		shape.drawLine(g, color);
	}

	public void drawLine(Coordinate[] cs, Color color, float strokeWidth) {
		if (cs == null || color == null) return;
		var shape = shapeOf(cs);
		shape.drawLine(g, color, strokeWidth);
	}

	@Override
	public void close() {
		g.dispose();
	}

	private Shape shapeOf(Coordinate[] cs) {
		int n = cs.length;
		var xs = new int[n];
		var ys = new int[n];
		for (int i = 0; i < n; i++) {
			var point = cs[i];
			xs[i] = margin + (int) ((point.getX() - minX) * scaleX);
			ys[i] = margin + (int) (height - ((point.getY() - minY) * scaleY));
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

		void drawLine(Graphics2D g2d, Color color, float strokeWidth) {
			var oldStroke = g2d.getStroke();
			g2d.setStroke(
				new BasicStroke(
					strokeWidth,
					BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND
				)
			);
			g2d.setColor(color);
			for (int j = 1; j < n; j++) {
				int i = j - 1;
				g2d.drawLine(xs[i], ys[i], xs[j], ys[j]);
			}
			g2d.setStroke(oldStroke);
		}
	}
}
