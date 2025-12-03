package com.greendelta.bioheating.graph;

import org.locationtech.jts.geom.LineString;

public record Edge(
	long id, Node source, Node target, LineString line, double length
) {

}
