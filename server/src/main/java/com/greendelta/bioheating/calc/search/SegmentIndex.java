package com.greendelta.bioheating.calc.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.strtree.STRtree;

import com.greendelta.bioheating.model.Street;

public class SegmentIndex {

	private final AtomicInteger ids = new AtomicInteger(0);
	private final GeometryFactory factory;
	private final List<Segment> segments = new ArrayList<>();
	private final STRtree tree = new STRtree();

	public SegmentIndex(GeometryFactory factory) {
		this.factory = Objects.requireNonNull(factory);
	}

	public SegmentIndex() {
		this(new GeometryFactory());
	}

	public void addSegmentsOf(Street street) {
		if (street == null)
			return;
		var cs = street.coordinates();
		if (cs == null || cs.length < 2)
			return;
		for (int i = 1; i < cs.length; i++) {
			var line = factory.createLineString(new Coordinate[]{cs[i - 1], cs[i]});
			var envelope = new Envelope(cs[i-1], cs[i]);
			var segment = new Segment(ids.incrementAndGet(), line, envelope);
			segments.add(segment);
			tree.insert(envelope, segment);
		}
	}

	public List<Segment> find(BuildingShape bs) {
		if (bs == null)
			return List.of();
		var env = bs.envelope();
		for (int i = 0; i < 100; i++) {
			env = env.copy();
			env.expandBy(10);
			var res = tree.query(env);
			if (res == null || res.isEmpty())
				continue;
			var matches = res.stream()
				.filter(Segment.class::isInstance)
				.map(Segment.class::cast)
				.toList();
			if (!matches.isEmpty())
				return matches;
		}
		return List.of();
	}

	public record Segment(int id, LineString line, Envelope envelope) {



	}

}
