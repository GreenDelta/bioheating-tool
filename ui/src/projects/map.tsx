import React, { useEffect, useRef, useState, useCallback } from "react";
import * as L from "leaflet";
import { GeoFeature, GeoMap, isBuilding } from "../model";
import { DeleteIcon } from "../components/icons";
import "leaflet-draw";
import "leaflet-lasso";

interface MapProps {
	data: GeoMap;
	onSelect: (fs: GeoFeature[]) => void;
}

export const Map: React.FC<MapProps> = ({ data, onSelect }) => {
	const divRef = useRef<HTMLDivElement>(null);
	const isDrawingRef = useRef(false);
	const { selection, handleSelect } = useFeatureSelection(onSelect);
	const { mapRef, layerRef } = useLeafletMap(divRef, data, selection);
	const deleteSelection = useDeleteSelection(data, layerRef, selection, handleSelect);
	const selectedBuildingCount = data.features.filter(feature =>
		isBuilding(feature) && selection.has(feature.properties?.id)
	).length;
	useMapInteractions(mapRef, layerRef, handleSelect, isDrawingRef);
	useFeatureStyling(layerRef, selection);
	usePolygonDrawing(mapRef, layerRef, data, handleSelect, isDrawingRef);
	return (
		<div style={{ position: "relative", width: "100%", height: 750 }}>
			<div ref={divRef} style={{ width: "100%", height: "100%" }} />
			<button
				type="button"
				className="btn btn-sm"
				disabled={selectedBuildingCount === 0}
				onClick={deleteSelection}
				title={
					selectedBuildingCount > 0
						? `Delete ${selectedBuildingCount} selected building(s)`
						: "Select building(s) to delete"
				}
				style={{
					position: "absolute",
					left: 10,
					bottom: 10,
					zIndex: 1000,
					width: 38,
					height: 38,
					padding: 0,
					borderRadius: "50%",
					backgroundColor:
						selectedBuildingCount > 0 ? "rgba(255,255,255,0.95)" : "rgba(255,255,255,0.8)",
					border:
						selectedBuildingCount > 0
							? "1px solid var(--bs-danger)"
							: "1px solid var(--bs-secondary)",
					display: "flex",
					alignItems: "center",
					justifyContent: "center",
					boxShadow: "0 1px 4px rgba(0,0,0,0.2)",
				}}>
				<DeleteIcon color={selectedBuildingCount > 0 ? "var(--bs-danger)" : "var(--bs-secondary)"} />
			</button>
		</div>
	);
};


function useFeatureSelection(onSelect: (fs: GeoFeature[]) => void) {
	const [selection, setSelection] = useState<Set<any>>(new Set());

	const handleSelect = useCallback((features: GeoFeature[]) => {
		const nextIds = new Set();
		if (!features) {
			setSelection(nextIds);
			onSelect([]);
			return;
		}
		for (const f of features) {
			const id = f.properties?.id;
			if (!id) {
				continue;
			}
			nextIds.add(id);
		}

		let shouldClear = false;
		setSelection(current => {
			let someNew = false;
			for (const id of nextIds) {
				if (!current.has(id)) {
					someNew = true;
					break;
				}
			}
			shouldClear = !someNew;
			return shouldClear ? new Set() : nextIds;
		});

		if (shouldClear) {
			onSelect([]);
		} else {
			onSelect(features);
		}
	}, [onSelect]);

	return { selection, handleSelect };
}


function useLeafletMap(
	divRef: React.RefObject<HTMLDivElement | null>,
	data: GeoMap,
	selection: Set<any>,
) {
	const mapRef = useRef<L.Map | null>(null);
	const layerRef = useRef<L.GeoJSON | null>(null);

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

	return { mapRef, layerRef };
}


function useMapInteractions(
	mapRef: React.RefObject<L.Map | null>,
	layerRef: React.RefObject<L.GeoJSON | null>,
	handleSelect: (features: GeoFeature[]) => void,
	isDrawingRef: React.RefObject<boolean>,
) {
	useEffect(() => {
		if (layerRef.current && mapRef.current) {
			// Remove existing event handlers
			layerRef.current.off("click");
			mapRef.current.off("lasso.finished");
			mapRef.current.off("click");

			// Add click handler on map to clear selection when clicking empty space
			mapRef.current.on("click", () => {
				if (isDrawingRef.current) {
					return;
				}
				handleSelect([]);
			});

			// Add event handlers with current selection state
			layerRef.current.on("click", evt => {
				L.DomEvent.stopPropagation(evt);
				const f = evt?.propagatedFrom?.feature;
				if (f) {
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
	}, [handleSelect, isDrawingRef]);
}


function useFeatureStyling(
	layerRef: React.RefObject<L.GeoJSON | null>,
	selection: Set<any>,
) {
	useEffect(() => {
		if (layerRef.current) {
			layerRef.current.setStyle(feature => styleOf(feature, selection));
		}
	}, [selection]);
}

function useDeleteSelection(
	data: GeoMap,
	layerRef: React.RefObject<L.GeoJSON | null>,
	selection: Set<any>,
	handleSelect: (features: GeoFeature[]) => void,
) {
	return useCallback(() => {
		const ids = new Set(
			Array.from(selection).filter(id => id !== null && id !== undefined)
		);
		if (ids.size === 0) {
			return;
		}

		data.features = data.features.filter(feature => {
			if (!isBuilding(feature)) {
				return true;
			}
			return !ids.has(feature.properties?.id);
		});

		const layer = layerRef.current;
		if (layer) {
			const removed: L.Layer[] = [];
			layer.eachLayer(item => {
				const feature = (item as any).feature as GeoFeature | undefined;
				if (!feature || !isBuilding(feature)) {
					return;
				}
				if (ids.has(feature.properties?.id)) {
					removed.push(item);
				}
			});
			for (const item of removed) {
				layer.removeLayer(item);
			}
		}

		handleSelect([]);
	}, [data, layerRef, selection, handleSelect]);
}

function usePolygonDrawing(
	mapRef: React.RefObject<L.Map | null>,
	layerRef: React.RefObject<L.GeoJSON | null>,
	data: GeoMap,
	handleSelect: (features: GeoFeature[]) => void,
	isDrawingRef: React.RefObject<boolean>,
) {
	useEffect(() => {
		const map = mapRef.current;
		const layer = layerRef.current;
		if (!map || !layer) {
			return;
		}

		const drawnItems = new L.FeatureGroup();
		map.addLayer(drawnItems);

		const drawControl = new L.Control.Draw({
			edit: {
				featureGroup: drawnItems,
				edit: false,
				remove: false,
			},
			draw: {
				polygon: {},
				polyline: false,
				rectangle: false,
				circle: false,
				marker: false,
				circlemarker: false,
			},
		});
		map.addControl(drawControl);

		const onDrawStart: L.LeafletEventHandlerFn = () => {
			isDrawingRef.current = true;
		};

		const onDrawStop: L.LeafletEventHandlerFn = () => {
			isDrawingRef.current = false;
		};

		const onCreated: L.LeafletEventHandlerFn = evt => {
			isDrawingRef.current = false;
			const created = evt as L.DrawEvents.Created;
			if (!created.layer || !(created.layer instanceof L.Polygon)) {
				return;
			}

			const feature = createBuilding(created.layer, data);
			data.features.push(feature);
			layer.addData(feature);
			handleSelect([feature]);
		};

		map.on(L.Draw.Event.DRAWSTART, onDrawStart);
		map.on(L.Draw.Event.DRAWSTOP, onDrawStop);
		map.on(L.Draw.Event.CREATED, onCreated);

		return () => {
			isDrawingRef.current = false;
			map.off(L.Draw.Event.DRAWSTART, onDrawStart);
			map.off(L.Draw.Event.DRAWSTOP, onDrawStop);
			map.off(L.Draw.Event.CREATED, onCreated);
			map.removeControl(drawControl);
			map.removeLayer(drawnItems);
		};
	}, [mapRef, layerRef, data, handleSelect, isDrawingRef]);
}

function createBuilding(layer: L.Polygon, data: GeoMap): GeoFeature {
	const nextId = nextBuildingId(data);
	const geoJson = layer.toGeoJSON() as GeoFeature;
	const coordinates = geoJson.geometry.type === "Polygon"
		? geoJson.geometry.coordinates
		: [];
	return {
		type: "Feature",
		geometry: {
			type: "Polygon",
			coordinates,
		},
		properties: {
			"@type": "building",
			id: nextId,
			name: `New building ${Math.abs(nextId)}`,
			height: 10,
			storeys: 1,
			groundArea: polygonAreaOf(layer),
			isHeated: true,
			isSupplyCenter: false,
			isIncluded: false,
			type: "OTHER",
			constructionAge: "UNKNOWN",
		},
	};
}

/// To distinguish newly created buildings from already existing buildings, they
/// get a negative ID.
function nextBuildingId(data: GeoMap): number {
	let nextId = -1;
	for (const feature of data.features || []) {
		const id = feature.properties?.id;
		if (typeof id === "number") {
			nextId = Math.min(nextId, id - 1);
		}
	}
	return nextId;
}

function polygonAreaOf(layer: L.Polygon): number {
	const latLngs = layer.getLatLngs();
	const ring = Array.isArray(latLngs[0]) ? latLngs[0] : latLngs;
	return typeof (L as any).GeometryUtil?.geodesicArea === "function"
		? (L as any).GeometryUtil.geodesicArea(ring)
		: 0;
}

function styleOf(feature: any, ids: Set<any>) {
	const f = feature as GeoFeature;
	const id = f.properties?.id;
	const isSelected = id && ids.has(id);
	const color = isSelected ? "#fff59d" : colorOf(f);
	const weight = isBuilding(f) ? 2 : 4;
	return {
		fillColor: color,
		weight: isSelected ? weight + 1 : weight,
		opacity: 1,
		color: color,
		dashArray: "",
		fillOpacity: isSelected ? 0.8 : 0.5,
	};
}

function colorOf(f: GeoFeature): string {
	const props = f.properties || {};
	if (isBuilding(f)) {
		if (props.isSupplyCenter) return "#ff9800";
		if (!props.isHeated) return "#607d8b";
		return props.isIncluded ? "#ec407a" : "#f8bbd0";
	}
	// street
	return props.isExcluded ? "#607d8b" : "#1976d2";
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
