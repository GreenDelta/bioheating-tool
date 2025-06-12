import React, { useEffect, useRef, useState } from 'react';
import * as L from 'leaflet';
import { GeoFeature, GeoMap } from '../model';
import "leaflet-lasso";

interface MapProps {
	data: GeoMap;
	onSelect: (f: GeoFeature) => void;
}

export const Map: React.FC<MapProps> = ({ data, onSelect }) => {

	const divRef = useRef<HTMLDivElement>(null);
	const mapRef = useRef<L.Map | null>(null);
	const layerRef = useRef<L.GeoJSON | null>(null);
	const [selection, setSelection] = useState<Set<any>>(new Set());

	const handleSelect = (features: GeoFeature[]) => {
		const nextIds = new Set();
		if (!features) {
			setSelection(nextIds);
			return;
		}
		for (const f of features) {
			const id = f.properties?.id;
			if (id) {
				nextIds.add(id);
			}
		}
		setSelection(nextIds);
		if (features.length === 1) {
			onSelect(features[0]);
		}
	};

	useEffect(() => {
		const div = divRef.current;
		if (div && !mapRef.current) {

			// init the map
			const map = L.map(div);
			mapRef.current = map;
			addTileLayer(map);

			// add the GeoJSON layer
			const layer = L.geoJSON(data.features || [], {
				style: (feature) => styleOf(feature, selection),
			}).addTo(map);
			layerRef.current = layer;
			const bounds = layer.getBounds();
			map.fitBounds(bounds, { padding: [20, 20] });

			// add lasso tool
			(L.control as any).lasso({
				intersect: true,
				title: 'Select multiple features'
			}).addTo(map);

			// handle click events on GeoJSON layer
			layer.on("click", evt => {
				const f = evt?.propagatedFrom?.feature;
				if (f) {
					handleSelect([f]);
				}
			});

			// handle lasso events
			map.on('lasso.finished', (evt: any) => {
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

		return () => {
			if (mapRef.current) {
				mapRef.current.remove();
				mapRef.current = null;
				layerRef.current = null;
			}
		};
	}, []);

	// Update feature styles when selection changes
	useEffect(() => {
		if (layerRef.current) {
			layerRef.current.setStyle((feature) => styleOf(feature, selection));
		}
	}, [selection]);

	return <div ref={divRef} style={{ width: "100%", height: 650 }} />;
};

function styleOf(feature: any, ids: Set<any>) {
	const id = feature.properties?.id;
	const isSelected = id && ids.has(id);

	return {
		fillColor: isSelected ? '#ff7800' : '#3388ff',
		weight: isSelected ? 3 : 2,
		opacity: 1,
		color: isSelected ? '#ff7800' : '#3388ff',
		dashArray: '',
		fillOpacity: isSelected ? 0.7 : 0.2
	};
};

function addTileLayer(map: L.Map) {
	/*
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
		}).addTo(mapInstanceRef.current);
		*/

	L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
		maxZoom: 19,
		attribution: '© Esri, Maxar, Earthstar Geographics'
	}).addTo(map);
}
