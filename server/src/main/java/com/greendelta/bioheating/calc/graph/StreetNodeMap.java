package com.greendelta.bioheating.calc.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import com.greendelta.bioheating.calc.graph.Node.StreetNode;

/// A spatial hash map for efficient lookup and creation of street nodes.
///
/// This class uses a grid-based spatial hashing approach to find existing nodes
/// within a 1-meter radius in O(1) average time, instead of O(n) linear search.
///
/// ## How it works
///
/// Coordinates are rounded to 1-meter grid cells. Each cell is identified by a
/// unique 64-bit key computed from the grid cell's x and y indices. When looking
/// up a coordinate, we check the cell it falls into plus the 8 neighboring cells
/// (a 3x3 grid) to handle coordinates near cell boundaries.
///
/// ## Key computation
///
/// The grid key combines x and y indices into a single `long` value:
///
///     key = (x << 32) | (y & 0xFFFFFFFFL)
///
/// This places the x-index in the upper 32 bits and the y-index in the lower
/// 32 bits, creating a unique key for each grid cell.
///
/// ## Safety with UTM coordinates
///
/// UTM (Universal Transverse Mercator) coordinates are in meters and have the
/// following ranges:
///
/// - **Easting (x):** 100,000 to 900,000 meters (within a zone)
/// - **Northing (y):** 0 to ~10,000,000 meters
///
/// Both values fit comfortably within a signed 32-bit integer range
/// (-2,147,483,648 to 2,147,483,647). Since we use `Math.round()` to convert
/// to grid indices, and UTM coordinates are already in meters, the resulting
/// indices are well within the 32-bit range. The bit masking with `0xFFFFFFFFL`
/// ensures correct handling of negative values (which could occur with other
/// coordinate systems), though UTM values are always positive.
class StreetNodeMap {

	private final Map<Long, StreetNode> grid = new HashMap<>();
	private final GeometryFactory factory;
	private final AtomicLong ids;

	/// Creates a new street node map.
	///
	/// @param factory the geometry factory for creating point geometries
	/// @param ids     the ID generator for new nodes
	public StreetNodeMap(GeometryFactory factory, AtomicLong ids) {
		this.factory = factory;
		this.ids = ids;
	}

	/// Gets an existing node within 1 meter of the given coordinate, or creates
	/// a new node if none exists.
	///
	/// @param coo the coordinate to look up
	/// @return an existing node within 1 meter, or a new node at the coordinate
	public StreetNode getOrCreate(Coordinate coo) {
		long gridX = Math.round(coo.x);
		long gridY = Math.round(coo.y);

		// Check this cell and 8 neighbors to handle boundary cases.
		// A coordinate at (100.9, 200.1) rounds to cell (101, 200), but a
		// node at (100.1, 200.9) rounds to cell (100, 201). These are within
		// 1 meter but in different cells, so we must check neighbors.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				long key = gridKey(gridX + dx, gridY + dy);
				var existing = grid.get(key);
				if (existing != null
					&& coo.distance(existing.center().getCoordinate()) < 1.0) {
					return existing;
				}
			}
		}

		var point = factory.createPoint(coo);
		var node = new StreetNode(ids.incrementAndGet(), point);
		grid.put(gridKey(gridX, gridY), node);
		return node;
	}

	/// Computes a unique grid cell key from x and y indices.
	///
	/// The x-index occupies the upper 32 bits, the y-index the lower 32 bits.
	private static long gridKey(long x, long y) {
		return (x << 32) | (y & 0xFFFFFFFFL);
	}

	/// Returns the number of nodes in this map.
	public int size() {
		return grid.size();
	}
}
