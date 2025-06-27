import React, { useState, useEffect } from 'react';
import { GeoFeature, Inclusion } from '../model';
import { StreetData } from './panel-data';
import { StringField, SelectField } from './fields';

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

			<SelectField
				label="Inclusion"
				value={data.inclusion}
				options={[
					{ value: Inclusion.OPTIONAL, label: "Optional" },
					{ value: Inclusion.REQUIRED, label: "Required" },
					{ value: Inclusion.EXCLUDED, label: "Excluded" }
				]}
				onChange={value => setData(data.copyWith({ inclusion: value }))}
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
