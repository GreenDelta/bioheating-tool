import React, { useEffect, useRef, useState, useCallback } from "react";
import * as L from "leaflet";
import { GeoFeature, GeoMap, isBuilding } from "../model";
import "leaflet-lasso";

interface MapProps {
	data: GeoMap;
	onSelect: (fs: GeoFeature[]) => void;
}

export const Map: React.FC<MapProps> = ({ data, onSelect }) => {
	const divRef = useRef<HTMLDivElement>(null);
	const mapRef = useRef<L.Map | null>(null);
	const layerRef = useRef<L.GeoJSON | null>(null);
	const [selection, setSelection] = useState<Set<any>>(new Set());

	const handleSelect = useCallback((features: GeoFeature[]) => {
		console.log(selection);
		const nextIds = new Set();
		if (!features) {
			setSelection(nextIds);
			onSelect([]);
			return;
		}

		let someNew = false;
		for (const f of features) {
			const id = f.properties?.id;
			if (!id) {
				continue;
			}
			nextIds.add(id);
			if (!selection.has(id)) {
				someNew = true;
			}
		}

		if (!someNew) {
			setSelection(new Set());
			onSelect([]);
		} else {
			setSelection(nextIds);
			onSelect(features);
		}
	}, [selection, onSelect]);

	useEffect(() => {
		const div = divRef.current;
		if (div && !mapRef.current) {
			// init the map
			const map = L.map(div);
			mapRef.current = map;
			addTileLayer(map);

			// add the GeoJSON layer
			const layer = L.geoJSON(data.features || [], {
				style: feature => styleOf(feature, selection),
			}).addTo(map);
			layerRef.current = layer;
			const bounds = layer.getBounds();
			map.fitBounds(bounds, { padding: [20, 20] });

			// add lasso tool
			(L.control as any)
				.lasso({
					intersect: true,
					title: "Select multiple features",
				})
				.addTo(map);
		}

		return () => {
			if (mapRef.current) {
				mapRef.current.remove();
				mapRef.current = null;
				layerRef.current = null;
			}
		};
	}, []);

	// Separate effect for updating event handlers
	useEffect(() => {
		if (layerRef.current && mapRef.current) {
			// Remove existing event handlers
			layerRef.current.off("click");
			mapRef.current.off("lasso.finished");

			// Add event handlers with current selection state
			layerRef.current.on("click", evt => {
				const f = evt?.propagatedFrom?.feature;
				if (f) {
					console.log(f.properties?.id);
					handleSelect([f]);
				}
			});

			mapRef.current.on("lasso.finished", (evt: any) => {
				if (!evt.layers) {
					return;
				}
				const features: GeoFeature[] = [];
				evt.layers.forEach((lay: any) => {
					const feature: GeoFeature = lay.feature;
					if (feature) {
						features.push(feature);
					}
				});
				handleSelect(features);
			});
		}
	}, [handleSelect]);

	// Update feature styles when selection changes
	useEffect(() => {
		if (layerRef.current) {
			layerRef.current.setStyle(feature => styleOf(feature, selection));
		}
	}, [selection]);

	return <div ref={divRef} style={{ width: "100%", height: 750 }} />;
};

function styleOf(feature: any, ids: Set<any>) {
	const f = feature as GeoFeature;
	const id = f.properties?.id;
	const isSelected = id && ids.has(id);
	const color = isSelected ? "#fff59d" : colorOf(f);
	return {
		fillColor: color,
		weight: isSelected ? 3 : 2,
		opacity: 1,
		color: color,
		dashArray: "",
		fillOpacity: isSelected ? 0.8 : 0.5,
	};
}

function colorOf(f: GeoFeature): string {
	const props = f.properties || {};
	const inclusion = props.inclusion;

	// building
	if (isBuilding(f)) {
		if (!props.isHeated) {
			return "#607d8b";
		}
		return inclusion === "REQUIRED" ? "#ec407a" : "#f8bbd0";
	}

	// street
	if (inclusion === "EXCLUDED") {
		return "#607d8b";
	}
	if (inclusion === "REQUIRED") {
		return "#ec407a";
	}
	return "#1976d2";
}

function addTileLayer(map: L.Map) {
	/*
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
		}).addTo(mapInstanceRef.current);
		*/

	L.tileLayer(
		"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
		{
			maxZoom: 19,
			attribution: "© Esri, Maxar, Earthstar Geographics",
		},
	).addTo(map);
}
