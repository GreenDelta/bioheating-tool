import React from 'react';
import { GeoFeature } from '../model';
import { StringField } from './fields';

export const StreetPanel = ({ feature }: { feature: GeoFeature }) => {
	const streetName = feature.properties?.name || '';

	return <div className="card">
		<div className="card-body">

			<StringField
				label="Street"
				value={streetName}
				onChange={value => {
					// Update the feature properties
					if (feature.properties) {
						feature.properties.name = value;
					}
				}}
			/>
		</div>
	</div>
};
