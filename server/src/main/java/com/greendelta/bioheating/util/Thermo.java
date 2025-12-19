package com.greendelta.bioheating.util;

public class Thermo {

	private Thermo() {
	}

	/// Calculates the mass flow rate required for a given heating load.
	///
	/// @param flowTemp    the flow temperature in °C
	/// @param returnTemp  the return temperature in °C
	/// @param heatingLoad the heating load in kW
	/// @return the mass flow rate in kg/s
	public static double massFlowOf(
		double flowTemp, double returnTemp, double heatingLoad) {
		double deltaTemp = flowTemp - returnTemp;
		if (deltaTemp <= 0 || heatingLoad <= 0)
			return 0;
		double cp = WaterProps.heatCapacityOf((flowTemp + returnTemp) / 2);
		return 1000 * heatingLoad / (deltaTemp * cp * 3600);
	}

}
