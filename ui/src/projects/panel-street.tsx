import React, { useState, useEffect } from 'react';
import { GeoFeature } from '../model';
import { StreetData } from './panel-data';
import { StringField, CheckboxField } from './fields';

export const StreetPanel = ({ feature }: { feature: GeoFeature }) => {
	const [data, setData] = useState<StreetData>(StreetData.of(feature));
	useEffect(() => {
		setData(StreetData.of(feature));
	}, [feature]);

	return <div className="card">
		<div className="card-body">
			<h6>Street Information</h6>

			<StringField
				label="Street Name"
				value={data.name}
				onChange={value => setData(data.copyWith({ name: value }))}
			/>

			<CheckboxField
				label="Is excluded"
				checked={data.isExcluded}
				onChange={checked => setData(data.copyWith({ isExcluded: checked }))}
			/>

			<button
				className="btn btn-primary"
				disabled={!data.isValid()}
				onClick={() => data.applyOn(feature)}>
				Update
			</button>
		</div>
	</div>
};
