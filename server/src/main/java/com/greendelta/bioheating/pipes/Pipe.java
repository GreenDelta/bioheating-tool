package com.greendelta.bioheating.graph;

import java.util.ArrayList;
import java.util.List;

public record Pipe(double diameter, String rgb) {

	static List<Pipe> getAll() {
		var pipes = new ArrayList<Pipe>();
		double di = 8;
		var diameters = new ArrayList<Double>();
		while (di < 300) {
			double inc;
			if (di >= 100) {
				inc = 10;
			} else if (di >= 40) {
				inc = 5;
			} else {
				inc = 2;
			}
			di += inc;
			diameters.add(di);
		}

		double minD = diameters.getFirst();
		double maxD = diameters.getLast();
		for (var d : diameters) {
			String color = colorFor(d, minD, maxD);
			pipes.add(new Pipe(d, color));
		}
		return pipes;
	}

	/// Maps diameter to an RGB hex color using HSL interpolation.
	/// Small diameters → blue (hue 240°), large → red (hue 0°)
	private static String colorFor(double diameter, double minD, double maxD) {
		// normalize to [0, 1] where 0 = smallest, 1 = largest
		double t = (diameter - minD) / (maxD - minD);

		// hue: 240° (blue) → 0° (red), going through cyan, green, yellow
		double hue = (1 - t) * 240;
		double saturation = 0.75;
		double lightness = 0.5;

		return hslToRgb(hue, saturation, lightness);
	}

	private static String hslToRgb(double h, double s, double l) {
		double c = (1 - Math.abs(2 * l - 1)) * s;
		double x = c * (1 - Math.abs((h / 60) % 2 - 1));
		double m = l - c / 2;

		double r, g, b;
		if (h < 60) {
			r = c;
			g = x;
			b = 0;
		} else if (h < 120) {
			r = x;
			g = c;
			b = 0;
		} else if (h < 180) {
			r = 0;
			g = c;
			b = x;
		} else if (h < 240) {
			r = 0;
			g = x;
			b = c;
		} else if (h < 300) {
			r = x;
			g = 0;
			b = c;
		} else {
			r = c;
			g = 0;
			b = x;
		}

		int ri = (int) Math.round((r + m) * 255);
		int gi = (int) Math.round((g + m) * 255);
		int bi = (int) Math.round((b + m) * 255);
		return String.format("#%02x%02x%02x", ri, gi, bi);
	}

}
