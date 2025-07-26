import React, { useState, useEffect } from "react";
import { GeoFeature, Inclusion, Fuel } from "../model";
import { BuildingData, BuildingProps } from "./panel-data";
import { StringField, NumberField, CheckboxField, SelectField } from "./fields";

interface Props {
	feature: GeoFeature;
	fuels: Fuel[];
	onChange: () => void;
}

export const BuildingPanel = ({ feature, fuels, onChange }: Props) => {
	const [data, setData] = useState<BuildingData>(BuildingData.of(feature));
	useEffect(() => {
		setData(BuildingData.of(feature));
	}, [feature]);

	const put = (change: BuildingProps) => {
		const next = data.copyWith(change);
		next.applyOn(feature);
		setData(next);
		onChange();
	};

	return (
		<div className="card">
			<div className="card-body">
				<StringField
					label="Building"
					value={data.name}
					onChange={value => put({ name: value })}
				/>

				<CheckboxField
					label="Is heated"
					checked={data.isHeated}
					onChange={checked => put({ isHeated: checked })}
				/>

				<NumberField
					label="Heat demand (kWh)"
					value={data.heatDemand}
					step="0.1"
					disabled={!data.isHeated}
					onChange={value => put({ heatDemand: value })}
				/>

				<SelectField
					label="Fuel"
					value={data.fuelId ? data.fuelId.toString() : ""}
					disabled={!data.isHeated}
					options={[
						{ value: "", label: "" },
						...fuels.map(fuel => ({
							value: fuel.id.toString(),
							label: `${fuel.name} (${fuel.unit})`,
						})),
					]}
					onChange={value =>
						put({ fuelId: value === "" ? undefined : parseInt(value) })
					}
				/>

				<SelectField
					label="Inclusion"
					value={!data.isHeated ? "Excluded" : data.inclusion}
					disabled={!data.isHeated}
					options={[
						{ value: Inclusion.EXCLUDED, label: "Excluded" },
						{ value: Inclusion.REQUIRED, label: "Included" },
					]}
					onChange={value => put({ inclusion: value })}
				/>

				<NumberField
					label="Height (m)"
					value={data.height}
					step="0.1"
					onChange={value => put({ height: value })}
				/>

				<NumberField
					label="Storeys"
					value={data.storeys}
					step="1"
					onChange={value => put({ storeys: value })}
				/>

				<StringField
					label="Roof Type"
					value={data.roofType}
					onChange={value => put({ roofType: value })}
				/>

				<StringField
					label="Function"
					value={data.function}
					onChange={value => put({ function: value })}
				/>

				<NumberField
					label="Ground Area (m²)"
					value={data.groundArea}
					step="0.1"
					onChange={value => put({ groundArea: value })}
				/>

				<NumberField
					label="Heated Area (m²)"
					value={data.heatedArea}
					step="0.1"
					onChange={value => put({ heatedArea: value })}
				/>

				<NumberField
					label="Volume (m³)"
					value={data.volume}
					step="0.1"
					onChange={value => put({ volume: value })}
				/>

				<hr />
				<h6>Address Information</h6>

				<StringField
					label="Country"
					value={data.country}
					onChange={value => put({ country: value })}
				/>

				<StringField
					label="Locality/City"
					value={data.locality}
					onChange={value => put({ locality: value })}
				/>

				<StringField
					label="Postal Code"
					value={data.postalCode}
					onChange={value => put({ postalCode: value })}
				/>

				<StringField
					label="Street"
					value={data.street}
					onChange={value => put({ street: value })}
				/>

				<StringField
					label="Street Number"
					value={data.streetNumber}
					onChange={value => put({ streetNumber: value })}
				/>

				<NumberField
					label="Climate Zone"
					value={data.climateZone}
					step="1"
					onChange={value => put({ climateZone: value })}
				/>
			</div>
		</div>
	);
};
