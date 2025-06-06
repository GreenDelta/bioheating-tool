package com.greendelta.bioheating.model;

import java.nio.ByteBuffer;

import org.locationtech.jts.geom.Coordinate;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CoordinateConverter implements AttributeConverter<Coordinate[], byte[]> {

	@Override
	public byte[] convertToDatabaseColumn(Coordinate[] cs) {
		if (cs == null || cs.length == 0)
			return null;
		int n = cs.length * 3 * Double.BYTES;
		var buffer = ByteBuffer.allocate(n);
		for (var c : cs) {
			buffer.putDouble(c.getX());
			buffer.putDouble(c.getY());
			buffer.putDouble(c.getZ());
		}
		return buffer.array();
	}

	@Override
	public Coordinate[] convertToEntityAttribute(byte[] bytes) {
		if (bytes == null || bytes.length == 0)
			return new Coordinate[0];

		var buffer = ByteBuffer.wrap(bytes);
		int n = bytes.length / (3 * Double.BYTES);
		var cs = new Coordinate[n];

		for (int i = 0; i < n; i++) {
			double x = buffer.getDouble();
			double y = buffer.getDouble();
			double z = buffer.getDouble();
			cs[i] = new Coordinate(x, y, z);
		}
		return cs;
	}
}
