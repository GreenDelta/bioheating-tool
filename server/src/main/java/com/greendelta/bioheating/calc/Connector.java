package com.greendelta.bioheating.calc;

import org.locationtech.jts.geom.LineString;

public record Connector(
	BuildingPolygon buildingPolygon,
	StreetLine streetLine,
	LineString connectorLine,
	double length
) {



}
