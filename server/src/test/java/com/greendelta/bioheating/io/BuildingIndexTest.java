package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.greendelta.bioheating.model.Building;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

class BuildingIndexTest {

	private final GeometryFactory geometries = new GeometryFactory();

	@Test
	void findsBuildingsByCityId() {
		var a = building("a", 0, 0, 10);
		var b = building("b", 100, 0, 10);
		var index = BuildingIndex.of(List.of(a, b));

		assertEquals(a, index.findByCityId("a"));
		assertEquals(b, index.findByCityId("b"));
		assertNull(index.findByCityId("c"));
		assertNull(index.findByCityId(null));
	}

	@Test
	void findsTheBuildingWithTheLargestOverlap() {
		var a = building("a", 0, 0, 10); // x: 0..10
		var b = building("b", 8, 0, 10); // x: 8..18
		var index = BuildingIndex.of(List.of(a, b));

		// x: 7..17 -> overlap with a = 3, with b = 9
		assertEquals(b, index.findIntersecting(square(7, 0, 10)));
	}

	@Test
	void returnsNullWhenNothingIntersects() {
		var index = BuildingIndex.of(List.of(building("a", 0, 0, 10)));
		assertNull(index.findIntersecting(square(100, 100, 10)));
	}

	@Test
	void countsHeatedNeighborsWithinTheThreshold() {
		var main = building("main", 0, 0, 10);
		var touching = building("touching", 10, 0, 10); // distance 0
		var near = building("near", 10.1, 0, 10); // distance 0.1
		var far = building("far", 20, 0, 10); // distance 10
		var index = BuildingIndex.of(List.of(main, touching, near, far));

		assertEquals(2, index.countHeatedNeighbors(main));
	}

	@Test
	void ignoresUnheatedNeighbors() {
		var main = building("main", 0, 0, 10, true);
		var heated = building("heated", 10, 0, 10, true);
		var cold = building("cold", -10.05, 0, 10, false);
		var index = BuildingIndex.of(List.of(main, heated, cold));

		assertEquals(1, index.countHeatedNeighbors(main));
	}

	@Test
	void ignoresTheBuildingItself() {
		var main = building("main", 0, 0, 10);
		var index = BuildingIndex.of(List.of(main));

		assertEquals(0, index.countHeatedNeighbors(main));
	}

	@Test
	void countsNeighborsForAGeometryThatIsNotIndexed() {
		var a = building("a", 0, 0, 10);
		var b = building("b", 10, 0, 10);
		var index = BuildingIndex.of(List.of(a, b));

		// overlaps a and b, so both are neighbors
		assertEquals(2, index.countHeatedNeighbors(square(9.95, 5, 0.1)));
	}

	@Test
	void handlesBuildingsWithoutCoordinates() {
		var noCoordinates = new Building().cityId("empty").isHeated(true);
		var a = building("a", 0, 0, 10);
		var index = BuildingIndex.of(List.of(noCoordinates, a));

		assertEquals(noCoordinates, index.findByCityId("empty"));
		assertEquals(a, index.findIntersecting(square(0, 0, 10)));
		assertEquals(0, index.countHeatedNeighbors(noCoordinates));
	}

	@Test
	void handlesEmptyOrNullInput() {
		var index = BuildingIndex.of(null);

		assertNull(index.findByCityId("a"));
		assertNull(index.findIntersecting(square(0, 0, 10)));
		assertEquals(0, index.countHeatedNeighbors(square(0, 0, 10)));
	}

	private Building building(String cityId, double x, double y, double size) {
		return building(cityId, x, y, size, true);
	}

	private Building building(
		String cityId, double x, double y, double size, boolean heated
	) {
		return new Building()
			.cityId(cityId)
			.coordinates(coordinates(x, y, size))
			.isHeated(heated);
	}

	private Polygon square(double x, double y, double size) {
		return geometries.createPolygon(coordinates(x, y, size));
	}

	private static Coordinate[] coordinates(double x, double y, double size) {
		return new Coordinate[] {
			new Coordinate(x, y),
			new Coordinate(x + size, y),
			new Coordinate(x + size, y + size),
			new Coordinate(x, y + size),
			new Coordinate(x, y),
		};
	}
}
