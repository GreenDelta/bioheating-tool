package com.greendelta.bioheating.calc;

import static org.junit.jupiter.api.Assertions.*;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.Street;

public class ConnectorTest {

	@Test
	public void testSimpleDistance() {
		var building = new Building().coordinates(
			Arrays.array(xy(1, 1), xy(5, 1), xy(5, 5), xy(1, 5), xy(1, 1)));
		var street = new Street().coordinates(
			Arrays.array(xy(7, 1), xy(7, 5)));

		var builder = GeometryBuilder.getDefault();
		var polygon = builder.polygonOf(building).orElseThrow();
		var line = builder.lineOf(street).orElseThrow();

		var connector = builder.connectorOf(polygon, line).orElseThrow();
		assertEquals(2.0, connector.length(), 1e-10);
	}

	@Test
	public void testSplitPoint() {
		var building = new Building().coordinates(
			Arrays.array(xy(2.5, 1), xy(5, 2.5), xy(2.5, 5), xy(1, 2.5), xy(2.5, 1)));
		var street = new Street().coordinates(
			Arrays.array(xy(7, 1), xy(7, 5)));

		var builder = GeometryBuilder.getDefault();
		var polygon = builder.polygonOf(building).orElseThrow();
		var line = builder.lineOf(street).orElseThrow();

		var connector = builder.connectorOf(polygon, line).orElseThrow();
		// System.out.println(connector.connectorLine());
		assertEquals(2.0, connector.length(), 1e-10);
	}

	private Coordinate xy(double x, double y) {
		return new Coordinate(x, y);
	}
}
