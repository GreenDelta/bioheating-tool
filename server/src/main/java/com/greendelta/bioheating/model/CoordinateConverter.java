package com.greendelta.bioheating.model;

import org.locationtech.jts.geom.Coordinate;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CoordinateConverter implements AttributeConverter<Coordinate[], byte[]> {

	@Override
	public byte[] convertToDatabaseColumn(Coordinate[] cs) {
		if (cs == null)
			return null;
		// TODO
		return new byte[0];
	}

	@Override
	public Coordinate[] convertToEntityAttribute(byte[] bytes) {
		if (bytes == null)
			return null;
		// TODO
		return new Coordinate[0];
	}
}
