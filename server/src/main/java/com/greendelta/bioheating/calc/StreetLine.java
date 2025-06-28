package com.greendelta.bioheating.calc;

import java.util.Objects;

import org.locationtech.jts.geom.LineString;

import com.greendelta.bioheating.model.Street;

public record StreetLine(Street street, LineString line) {

	public StreetLine {
		Objects.requireNonNull(street);
		Objects.requireNonNull(line);
	}
}
