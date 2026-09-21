package com.greendelta.bioheating.predict;

/// A linear correction `y = a * x + b` that is applied to a raw output of the
/// prediction model. This is an application-side calibration and is not part
/// of the model training.
///
/// @param a the slope
/// @param b the offset
public record Correction(double a, double b) {

	/// A correction that does not change the model output.
	public static final Correction NONE = new Correction(1, 0);

	/// Applies this correction to the given model value. The result is never
	/// negative because a heat demand or a peak load cannot be negative.
	public double apply(double x) {
		return Math.max(0, a * x + b);
	}
}
