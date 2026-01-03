package com.greendelta.bioheating.pipes;

import java.util.ArrayList;
import java.util.List;

/// Represents a pipe with its physical properties.
///
/// @param innerDiameter the inner diameter of the pipe in millimeters (mm)
/// @param rgb           the RGB hex color code for visualization (e.g., "#ff0000")
public record Pipe(double innerDiameter, String rgb) {

	/// Calculates the U-Value (heat transfer coefficient) of the pipe.
	///
	/// The U-Value is calculated using a linear regression formula:
	/// `y = 0.00145663 * x + 0.0994454`
	/// where x is the inner diameter in mm.
	///
	/// @return the U-Value in W/(m·K)
	public double uValue() {
		return 0.00145663 * innerDiameter + 0.0994454;
	}

	static List<Pipe> getAll() {
		var pipes = new ArrayList<Pipe>();
		double di = 8;
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
			pipes.add(new Pipe(di, colorFor(di)));
		}
		return pipes;
	}

	private static String colorFor(double diameter) {
		if (diameter < 20)
			return "#E1BEE7";
		if (diameter < 30)
			return "#CE93D8";
		if (diameter < 40)
			return "#BA68C8";
		if (diameter < 50)
			return "#AB47BC";
		if (diameter < 75)
			return "#8E24AA";
		if (diameter < 100)
			return "#7B1FA2";
		if (diameter < 125)
			return "#6A1B9A";
		if (diameter < 150)
			return "#4A148C";
		if (diameter < 175)
			return "#311B92";
		return "#1A237E";
	}

}
