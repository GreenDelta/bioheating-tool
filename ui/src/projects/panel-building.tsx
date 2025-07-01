import React, { useState, useEffect } from 'react';
import { GeoFeature, Inclusion } from '../model';
import { BuildingData } from './panel-data';
import { StringField, NumberField, CheckboxField, SelectField } from './fields';

export const BuildingPanel = ({ feature }: { feature: GeoFeature }) => {
	const [data, setData] = useState<BuildingData>(BuildingData.of(feature));
	useEffect(() => {
		setData(BuildingData.of(feature));
	}, [feature]);

	return <div className="card">
		<div className="card-body">

			<StringField label="Building" value={data.name}
				onChange={value => setData(data.copyWith({ name: value }))} />

			<SelectField
				label="Inclusion"
				value={data.inclusion}
				options={[
					{ value: Inclusion.EXCLUDED, label: "Excluded" },
					{ value: Inclusion.REQUIRED, label: "Included" },
				]}
				onChange={value => setData(data.copyWith({ inclusion: value }))}
			/>

			<NumberField label="Height (m)" value={data.height} step="0.1"
				onChange={value => setData(data.copyWith({ height: value }))} />

			<NumberField label="Storeys" value={data.storeys} step="1"
				onChange={value => setData(data.copyWith({ storeys: value }))} />

			<CheckboxField label="Is heated" checked={data.isHeated}
				onChange={checked => setData(data.copyWith({ isHeated: checked }))} />

			<NumberField label="Heat demand (kWh)" value={data.heatDemand} step="0.1"
				disabled={!data.isHeated}
				onChange={value => setData(data.copyWith({ heatDemand: value }))} />

			<StringField label="Roof Type" value={data.roofType}
				onChange={value => setData(data.copyWith({ roofType: value }))} />

			<StringField label="Function" value={data.function}
				onChange={value => setData(data.copyWith({ function: value }))} />

			<NumberField label="Ground Area (m²)" value={data.groundArea} step="0.1"
				onChange={value => setData(data.copyWith({ groundArea: value }))} />

			<NumberField label="Heated Area (m²)" value={data.heatedArea} step="0.1"
				onChange={value => setData(data.copyWith({ heatedArea: value }))} />

			<NumberField
				label="Volume (m³)" value={data.volume} step="0.1"
				onChange={value => setData(data.copyWith({ volume: value }))} />

			<hr />
			<h6>Address Information</h6>

			<StringField label="Country" value={data.country}
				onChange={value => setData(data.copyWith({ country: value }))} />

			<StringField label="Locality/City" value={data.locality}
				onChange={value => setData(data.copyWith({ locality: value }))} />

			<StringField label="Postal Code" value={data.postalCode}
				onChange={value => setData(data.copyWith({ postalCode: value }))} />

			<StringField label="Street" value={data.street}
				onChange={value => setData(data.copyWith({ street: value }))} />

			<StringField label="Street Number" value={data.streetNumber}
				onChange={value => setData(data.copyWith({ streetNumber: value }))} />

			<NumberField label="Climate Zone" value={data.climateZone} step="1"
				onChange={value => setData(data.copyWith({ climateZone: value }))} />

			<button
				className="btn btn-primary"
				disabled={!data.isValid()}
				onClick={() => data.applyOn(feature)}>
				Update
			</button>
		</div>
	</div>
};
