package com.greendelta.bioheating.citygml;

import java.util.Optional;

import org.citygml4j.core.model.core.CityModel;

public record GmlCRS(String name, int dimensions) {

	public static Optional<GmlCRS> of(CityModel model) {
		if (model == null)
			return Optional.empty();
		var bounds = model.getBoundedBy();
		if (bounds == null)
			return Optional.empty();
		var envelope = bounds.getEnvelope();
		if (envelope == null)
			return Optional.empty();
		var name = envelope.getSrsName();
		if (name == null || name.isBlank())
			return Optional.empty();
		var dimensions = envelope.getSrsDimension();
		return dimensions != null && dimensions > 0
			? Optional.of(new GmlCRS(name, dimensions))
			: Optional.empty();
	}

	@Override
	public String toString() {
		return name + "; " + dimensions + "D";
	}
}

