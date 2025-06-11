import React, { useEffect, useRef } from 'react';
import * as L from 'leaflet';
import { GeoMap } from '../model';

interface MapProps {
	data: GeoMap;
}

export const Map: React.FC<MapProps> = ({ data }) => {

	const divRef = useRef<HTMLDivElement>(null);
	const mapRef = useRef<L.Map | null>(null);

	useEffect(() => {
		if (divRef.current && !mapRef.current) {
			mapRef.current = initMap(divRef.current, data);
		}

		return () => {
			if (mapRef.current) {
				mapRef.current.remove();
				mapRef.current = null;
			}
		};
	}, []);
	return <div ref={divRef} style={{width: "100%", height: 650}} />;
};

function initMap(div: HTMLDivElement, data: GeoMap): L.Map {
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
		return map;
	}

	const features = L.geoJSON(data.features).addTo(map);
	const bounds = features.getBounds();
	map.fitBounds(bounds, { padding: [20, 20] });

	return map;
}
