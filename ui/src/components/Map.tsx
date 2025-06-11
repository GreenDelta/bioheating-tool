import React, { useEffect, useRef } from 'react';
import * as L from 'leaflet';
import { GeoFeature } from '../model';

interface MapProps {
	style?: React.CSSProperties;
	center?: [number, number];
	zoom?: number;
	features?: GeoFeature[];
}

export const Map: React.FC<MapProps> = ({
	style = { height: '400px', width: '100%' },
	center = [51.505, -0.09], // Default to London
	zoom = 13,
	features = []
}) => {
	const mapRef = useRef<HTMLDivElement>(null);
	const mapInstanceRef = useRef<L.Map | null>(null);
	const featureLayerRef = useRef<L.GeoJSON | null>(null);

	useEffect(() => {

		if (mapRef.current && !mapInstanceRef.current) {

			mapInstanceRef.current = L.map(mapRef.current).setView(center, zoom);

			/*
			L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
				attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
			}).addTo(mapInstanceRef.current);
			*/

			L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
				maxZoom: 19,
				attribution: '© Esri, Maxar, Earthstar Geographics'
			}).addTo(mapInstanceRef.current);

		}

		return () => {
			// Cleanup map when component unmounts
			if (mapInstanceRef.current) {
				mapInstanceRef.current.remove();
				mapInstanceRef.current = null;
			}
		};
	}, []);

	useEffect(() => {
		// Update map center and zoom when props change
		if (mapInstanceRef.current) {
			mapInstanceRef.current.setView(center, zoom);
		}
	}, [center, zoom]);

	useEffect(() => {
		// Handle GeoJSON features
		if (mapInstanceRef.current) {
			// Remove existing feature layer if it exists
			if (featureLayerRef.current) {
				mapInstanceRef.current.removeLayer(featureLayerRef.current);
				featureLayerRef.current = null;
			}

			// Add new feature layer if features are provided
			if (features && features.length > 0) {
				featureLayerRef.current = L.geoJSON(features, {
					style: {
						fillColor: '#ff7800',
						weight: 2,
						opacity: 1,
						color: 'white',
						dashArray: '3',
						fillOpacity: 0.7
					},

					onEachFeature: (feature, layer) => {
						// Add popup with building information
						if (feature.properties) {
							const popupContent = `
                <div>
                  <strong>Building ID:</strong> ${feature.properties.id || 'N/A'}<br>
                  <strong>Name:</strong> ${feature.properties.name || 'N/A'}
                </div>
              `;
							layer.bindPopup(popupContent);
						}
					}

				});
				featureLayerRef.current.addTo(mapInstanceRef.current);

				// Fit map to bounds of features
				try {
					const bounds = featureLayerRef.current.getBounds();
					if (bounds.isValid()) {
						mapInstanceRef.current.fitBounds(bounds, { padding: [20, 20] });
					}
				} catch (error) {
					console.warn('Could not fit map to feature bounds:', error);
				}
			}
		}
	}, [features]);

	return <div ref={mapRef} style={style} />;
};
