import React, { useState, useEffect } from 'react';
import { GeoFeature, Inclusion, isBuilding, isStreet } from '../model';
import { SelectField } from './fields';

interface Props {
	features: GeoFeature[];
}

export const MultiPanel: React.FC<Props> = ({ features }) => {
	const [buildingInclusion, setBuildingInclusion] = useState<string>('');
	const [streetInclusion, setStreetInclusion] = useState<string>('');

	const buildings = features.filter(isBuilding);
	const streets = features.filter(isStreet);

	useEffect(() => {
		// Reset selections when features change
		setBuildingInclusion('');
		setStreetInclusion('');
	}, [features]);

	const handleBuildingInclusionChange = (value: string) => {
		setBuildingInclusion(value);

		// Update all selected buildings
		const updatedFeatures = features.map(feature => {
			if (isBuilding(feature)) {
				const updatedFeature = { ...feature };
				if (!updatedFeature.properties) {
					updatedFeature.properties = {};
				}
				updatedFeature.properties.inclusion = value;
				return updatedFeature;
			}
			return feature;
		});
	};

	const handleStreetInclusionChange = (value: string) => {
		setStreetInclusion(value);

		// Update all selected streets
		const updatedFeatures = features.map(feature => {
			if (isStreet(feature)) {
				const updatedFeature = { ...feature };
				if (!updatedFeature.properties) {
					updatedFeature.properties = {};
				}
				updatedFeature.properties.inclusion = value;
				return updatedFeature;
			}
			return feature;
		});
	};

	return (
		<div className="card">
			<div className="card-body">

				{buildings.length > 0 && (
					<div className="mb-3">
						<h6 className="text-muted">Buildings ({buildings.length})</h6>
						<SelectField
							label="Set inclusion"
							value={buildingInclusion}
							options={[
								{ value: '', label: 'Select...' },
								{ value: Inclusion.REQUIRED, label: 'Included' },
								{ value: Inclusion.EXCLUDED, label: 'Excluded' }
							]}
							onChange={handleBuildingInclusionChange}
						/>
					</div>
				)}

				<StreetSection features={features} />

			</div>
		</div>
	);
};

const StreetSection = ({ features }: Props) => {
	const streets = features.filter(isStreet);
	if (!streets || streets.length === 0)
		return <></>;

	const [inclusion, setInclusion] = useState(commonInclusionOf(streets));
	const onUpdate = (v: string) => {
		const next = v === "" ? Inclusion.OPTIONAL : v;
		putInclusion(streets, next);
		setInclusion(next);
	};

	return (
		<div className="mb-3">
			<h6 className="text-muted">{streets.length} Streets</h6>
			<SelectField
				label="Set inclusion"
				value={inclusion}
				options={[
					{ value: '', label: 'Select...' },
					{ value: Inclusion.OPTIONAL, label: 'Optional' },
					{ value: Inclusion.REQUIRED, label: 'Required' },
					{ value: Inclusion.EXCLUDED, label: 'Excluded' }
				]}
				onChange={onUpdate}
			/>
		</div>
	);
}

function putInclusion(features: GeoFeature[], value: string) {
	for (const f of features) {
		if (!f.properties) {
			f.properties = {};
		}
		f.properties.inclusion = value;
	}
}

function commonInclusionOf(features: GeoFeature[]): string {
	if (!features || features.length === 0) {
		return "";
	}
	let v = features[0].properties?.inclusion || "";
	for (let i = 1; i < features.length; i++) {
		const vi = features[i].properties?.inclusion;
		if (vi !== v) {
			return "";
		}
	}
	return v;
}
