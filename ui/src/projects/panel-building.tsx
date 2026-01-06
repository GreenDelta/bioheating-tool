import React, { useState, useEffect } from "react";
import { GeoFeature, BuildingType, ConstructionAge, constructionAgeToString } from "../model";
import { BuildingData, BuildingProps } from "./panel-data";
import { StringField, NumberField, CheckboxField, SelectField } from "./fields";

interface Props {
	feature: GeoFeature;
	onChange: () => void;
}

export const BuildingPanel = ({ feature, onChange }: Props) => {
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

				<CheckboxField
					label="Is supply center"
					checked={data.isSupplyCenter}
					onChange={checked => put({ isSupplyCenter: checked })}
				/>

				<NumberField
					label="Heat demand (kWh)"
					value={data.heatDemand}
					step="0.1"
					disabled={!data.isHeated}
					onChange={value => put({ heatDemand: value })}
				/>

				<NumberField
					label="Peak load (kW)"
					value={data.peakLoad}
					step="0.1"
					disabled={!data.isHeated}
					onChange={value => put({ peakLoad: value })}
				/>

				{!data.isSupplyCenter && data.isHeated && (
					<CheckboxField
						label="Include in solution"
						checked={data.isIncluded}
						onChange={checked => put({ isIncluded: checked })}
					/>
				)}

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
						{ value: ConstructionAge.UNKNOWN, label: constructionAgeToString(ConstructionAge.UNKNOWN) },
						{ value: ConstructionAge.AGE_1900_1919, label: constructionAgeToString(ConstructionAge.AGE_1900_1919) },
						{ value: ConstructionAge.AGE_1919_1948, label: constructionAgeToString(ConstructionAge.AGE_1919_1948) },
						{ value: ConstructionAge.AGE_1949_1978, label: constructionAgeToString(ConstructionAge.AGE_1949_1978) },
						{ value: ConstructionAge.AGE_1979_1995, label: constructionAgeToString(ConstructionAge.AGE_1979_1995) },
						{ value: ConstructionAge.AGE_1995_2009, label: constructionAgeToString(ConstructionAge.AGE_1995_2009) },
						{ value: ConstructionAge.AGE_2010_2030, label: constructionAgeToString(ConstructionAge.AGE_2010_2030) },
					]}
					onChange={value => put({ constructionAge: value })}
				/>

				<NumberField
					label="Height (m)"
					value={data.height}
					step="0.1"
					onChange={value => put({ height: value })}
				/>

				<NumberField
					label="Ground Area (m²)"
					value={data.groundArea}
					step="0.1"
					onChange={value => put({ groundArea: value })}
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

			</div>
		</div>
	);
};
