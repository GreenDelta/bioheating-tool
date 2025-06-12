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
	const featuresLayerRef = useRef<L.GeoJSON | null>(null);
	const [selectedFeatureIds, setSelectedFeatureIds] = useState<Set<string>>(new Set());

	useEffect(() => {
		if (divRef.current && !mapRef.current) {
			const { map, featuresLayer } = initMap(divRef.current, data, onSelect, setSelectedFeatureIds);
			mapRef.current = map;
			featuresLayerRef.current = featuresLayer;
		}

		return () => {
			if (mapRef.current) {
				mapRef.current.remove();
				mapRef.current = null;
				featuresLayerRef.current = null;
			}
		};
	}, []);

	// Update feature styles when selection changes
	useEffect(() => {
		if (featuresLayerRef.current) {
			featuresLayerRef.current.setStyle((feature) => getFeatureStyle(feature, selectedFeatureIds));
		}
	}, [selectedFeatureIds]);

	return <div ref={divRef} style={{ width: "100%", height: 650 }} />;
};

// Style function for features based on selection state
const getFeatureStyle = (feature: any, selectedIds: Set<string>) => {
	const featureId = feature.properties?.id;
	const isSelected = featureId && selectedIds.has(featureId);

	return {
		fillColor: isSelected ? '#ff7800' : '#3388ff',
		weight: isSelected ? 3 : 2,
		opacity: 1,
		color: isSelected ? '#ff7800' : '#3388ff',
		dashArray: '',
		fillOpacity: isSelected ? 0.7 : 0.2
	};
};

function initMap(
	div: HTMLDivElement,
	data: GeoMap,
	onSelect: (f: GeoFeature) => void,
	setSelectedFeatureIds: React.Dispatch<React.SetStateAction<Set<string>>>
): { map: L.Map, featuresLayer: L.GeoJSON | null } {

	const map = L.map(div);

	/*
	L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
		attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
	}).addTo(mapInstanceRef.current);
	*/

	L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
		maxZoom: 19,
		attribution: '© Esri, Maxar, Earthstar Geographics'
	}).addTo(map);

	if (!data || !data.features) {
		return { map, featuresLayer: null };
	}

	const features = L.geoJSON(data.features, {
		style: (feature) => getFeatureStyle(feature, new Set()),
		onEachFeature: (feature, layer) => {
			// Add feature ID if it doesn't exist
			if (!feature.properties?.id) {
				feature.properties = feature.properties || {};
				feature.properties.id = `feature_${Math.random().toString(36).substr(2, 9)}`;
			}
		}
	}).addTo(map);

	const bounds = features.getBounds();
	map.fitBounds(bounds, { padding: [20, 20] });

	// Handle single click selection
	features.on("click", evt => {
		if (!evt || !evt.propagatedFrom) {
			return;
		}
		const feature: GeoFeature = evt.propagatedFrom.feature;
		if (feature) {
			// Clear previous selection and select this feature
			const featureId = feature.properties?.id;
			if (featureId) {
				setSelectedFeatureIds(new Set([featureId]));
			}
			onSelect(feature);
		}
	});

	// Add lasso control with visible UI button
	(L.control as any).lasso({
		intersect: true,
		title: 'Select multiple features'
	}).addTo(map);

	// Listen for lasso selection events
	map.on('lasso.finished', (event: any) => {
		console.log('Lasso selection completed:', event.layers);

		// Clear previous selection and select lassoed features
		const newSelectedIds = new Set<string>();
		event.layers.forEach((layer: any) => {
			const feature = layer.feature;
			if (feature) {
				const featureId = feature.properties?.id;
				if (featureId) {
					newSelectedIds.add(featureId);
				}
			}
		});
		setSelectedFeatureIds(newSelectedIds);

		// If only one feature selected, call onSelect
		if (event.layers.length === 1) {
			const feature = event.layers[0].feature;
			if (feature) {
				onSelect(feature);
			}
		}
	});

	return { map, featuresLayer: features };
}
