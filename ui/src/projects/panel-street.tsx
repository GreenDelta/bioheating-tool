import React, { useState, useEffect } from 'react';
import { GeoFeature } from '../model';
import { StringField, CheckboxField } from './fields';

export const StreetPanel = ({ feature }: { feature: GeoFeature }) => {
	const [streetName, setStreetName] = useState(feature.properties?.name || '');
	const [isExcluded, setIsExcluded] = useState(feature.properties?.isExcluded || false);

	useEffect(() => {
		setStreetName(feature.properties?.name || '');
		setIsExcluded(feature.properties?.isExcluded || false);
	}, [feature]);

	const updateFeature = () => {
		if (feature.properties) {
			feature.properties.name = streetName;
			feature.properties.isExcluded = isExcluded;
		}
	};

	return <div className="card">
		<div className="card-body">
			<h6>Street Information</h6>

			<StringField
				label="Street Name"
				value={streetName}
				onChange={value => setStreetName(value)}
			/>

			<CheckboxField
				label="Is excluded"
				checked={isExcluded}
				onChange={checked => setIsExcluded(checked)}
			/>

			<button
				className="btn btn-primary"
				onClick={updateFeature}>
				Update
			</button>
		</div>
	</div>
};
