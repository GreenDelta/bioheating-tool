package com.greendelta.bioheating.math;

public class PipeConfig {

	/// Maximum pressure loss in Pa/m.
	public static final double MAX_PRESSURE_LOSS = 100;

	/// Maximum flow velocity in m/s.
	public static final double MAX_FLOW_VELOCITY = 3;

	/// Surcharge factor for fittings/installations (20%).
	public static final double FITTING_SURCHARGE = 0.20;

	/// Roughness of plastic pipes in m.
	public static final double ROUGHNESS_PLASTIC = 0.002e-3;

	/// Roughness of smooth steel pipes in m.
	public static final double ROUGHNESS_STEEL_SMOOTH = 0.01e-3;

	private double flowTemperature;
	private double returnTemperature;
	private double roughness;

	public PipeConfig withFlowTemperature(double flowTemp) {
		this.flowTemperature = flowTemp;
		return this;
	}

	public double flowTemperature() {
		return flowTemperature;
	}

	public PipeConfig withReturnTemperature(double returnTemp) {
		this.returnTemperature = returnTemp;
		return this;
	}

	public double returnTemperature() {
		return returnTemperature;
	}

	public PipeConfig withRoughness(double roughness) {
		this.roughness = roughness;
		return this;
	}

	public double roughness() {
		return roughness;
	}
}
