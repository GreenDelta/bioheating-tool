import React, { useState, useEffect } from "react";
import { GeoFeature, Inclusion, Fuel, BuildingType } from "../model";
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
					value={data.roofTypeLabel}
					onChange={value => put({ roofTypeLabel: value })}
				/>

				<StringField
					label="Function"
					value={data.functionLabel}
					onChange={value => put({ functionLabel: value })}
				/>

				<SelectField
					label="Building Type"
					value={data.type}
					options={[
						{ value: BuildingType.HIGH_RISE, label: "High Rise" },
						{ value: BuildingType.MULTI_FAMILY_SMALL, label: "Multi Family Small" },
						{ value: BuildingType.MULTI_FAMILY_MEDIUM, label: "Multi Family Medium" },
						{ value: BuildingType.MULTI_FAMILY_LARGE, label: "Multi Family Large" },
						{ value: BuildingType.BUILDING_PART, label: "Building Part" },
						{ value: BuildingType.SINGLE_FAMILY, label: "Single Family" },
						{ value: BuildingType.END_TERRACE, label: "End Terrace" },
						{ value: BuildingType.MID_TERRACE, label: "Mid Terrace" },
						{ value: BuildingType.HOUSE_GROUP, label: "House Group" },
						{ value: BuildingType.OTHER, label: "Other" },
					]}
					onChange={value => put({ type: value })}
				/>

				<SelectField
					label="Construction Age"
					value={data.constructionAge}
					options={[
						{ value: "", label: "(empty)" },
						{ value: "1900-1919", label: "1900-1919" },
						{ value: "1919-1948", label: "1919-1948" },
						{ value: "1949-1978", label: "1949-1978" },
						{ value: "1979-1995", label: "1979-1995" },
						{ value: "1995-2009", label: "1995-2009" },
						{ value: "2010-2030", label: "2010-2030" },
					]}
					onChange={value => put({ constructionAge: value })}
				/>

				<NumberField
					label="Height (m)"
					value={data.height}
					step="0.1"
					onChange={value => put({ height: value })}
				/>				<NumberField
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
