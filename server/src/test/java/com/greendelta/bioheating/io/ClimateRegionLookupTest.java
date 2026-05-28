package com.greendelta.bioheating.io;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import com.greendelta.bioheating.model.Building;
import com.greendelta.bioheating.model.ClimateRegion;
import com.greendelta.bioheating.model.GeoMap;
import com.greendelta.bioheating.model.Project;

public class ClimateRegionLookupTest {

	@Test
	public void lookupUsesFirstBuildingPointInProjectCrs() {
		var area = ClimateRegionLookup
			.loadRegionAreas()
			.orElseThrow()
			.stream()
			.filter(candidate -> candidate.number() == 14)
			.findFirst()
			.orElseThrow();

		var wgs84Point = area.geometry().getInteriorPoint().getCoordinate();
		var utm = CrsId.utmFromWGS84(wgs84Point.x, wgs84Point.y).orElseThrow();
		var point = CoordinateTransformer
			.fromWgs84To(utm.value())
			.orElseThrow()
			.project(wgs84Point.x, wgs84Point.y)
			.orElseThrow();

		double delta = 5.0;
		var building = new Building().coordinates(new Coordinate[] {
			new Coordinate(point.x - delta, point.y - delta),
			new Coordinate(point.x + delta, point.y - delta),
			new Coordinate(point.x + delta, point.y + delta),
			new Coordinate(point.x - delta, point.y + delta),
			new Coordinate(point.x - delta, point.y - delta),
		});

		var project = new Project().map(new GeoMap().crs(utm.value()));
		project.map().buildings().add(building);

		var expected = new ClimateRegion().number(14);
		var other = new ClimateRegion().number(13);

		var region = new ClimateRegionLookup()
			.lookup(project, List.of(other, expected))
			.orElseThrow();

		assertSame(expected, region);
		assertSame(expected, project.climateRegion());
	}
}
