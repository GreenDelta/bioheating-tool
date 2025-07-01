import React, { useState, useEffect } from 'react';
import { GeoFeature, Inclusion, isBuilding, isStreet } from '../model';
import { SelectField } from './fields';

interface MultiPanelProps {
	features: GeoFeature[];
}

export const MultiPanel: React.FC<MultiPanelProps> = ({ features }) => {
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
				<h6>Multi-Selection ({features.length} items)</h6>

				{buildings.length > 0 && (
					<div className="mb-3">
						<h6 className="text-muted">Buildings ({buildings.length})</h6>
						<SelectField
							label="Set Inclusion"
							value={buildingInclusion}
							options={[
								{ value: '', label: 'Select...' },
								{ value: Inclusion.REQUIRED, label: 'Required' },
								{ value: Inclusion.EXCLUDED, label: 'Excluded' }
							]}
							onChange={handleBuildingInclusionChange}
						/>
					</div>
				)}

				{streets.length > 0 && (
					<div className="mb-3">
						<h6 className="text-muted">Streets ({streets.length})</h6>
						<SelectField
							label="Set Inclusion"
							value={streetInclusion}
							options={[
								{ value: '', label: 'Select...' },
								{ value: Inclusion.OPTIONAL, label: 'Optional' },
								{ value: Inclusion.REQUIRED, label: 'Required' },
								{ value: Inclusion.EXCLUDED, label: 'Excluded' }
							]}
							onChange={handleStreetInclusionChange}
						/>
					</div>
				)}

				<div className="mt-3">
					<small className="text-muted">
						Selected: {buildings.length} building{buildings.length !== 1 ? 's' : ''}, {streets.length} street{streets.length !== 1 ? 's' : ''}
					</small>
				</div>
			</div>
		</div>
	);
};
