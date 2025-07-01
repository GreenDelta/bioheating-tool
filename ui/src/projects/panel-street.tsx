import React, { useState, useEffect } from 'react';
import { GeoFeature, Inclusion } from '../model';
import { StreetData, StreetProps } from './panel-data';
import { StringField, SelectField } from './fields';

export const StreetPanel = ({ feature }: { feature: GeoFeature }) => {
	const [data, setData] = useState<StreetData>(StreetData.of(feature));
	useEffect(() => {
		setData(StreetData.of(feature));
	}, [feature]);

	const put = (change: StreetProps) => {
		const next = data.copyWith(change);
		next.applyOn(feature);
		setData(next);
	};

	return <div className="card">
		<div className="card-body">
			<h6>Street Information</h6>

			<StringField
				label="Street Name"
				value={data.name}
				onChange={value => put({ name: value })}
			/>

			<SelectField
				label="Inclusion"
				value={data.inclusion}
				options={[
					{ value: Inclusion.OPTIONAL, label: "Optional" },
					{ value: Inclusion.REQUIRED, label: "Required" },
					{ value: Inclusion.EXCLUDED, label: "Excluded" }
				]}
				onChange={value => put({ inclusion: value })}
			/>

		</div>
	</div>
};
