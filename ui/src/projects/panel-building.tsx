import React, { useState, useEffect } from "react";
import * as api from "../api";
import {
	GeoFeature,
	GeoMap,
	BuildingType,
	ConstructionAge,
	constructionAgeToString,
	isBuilding,
} from "../model";
import { BuildingData, BuildingProps } from "./panel-data";
import { StringField, NumberField, CheckboxField, SelectField } from "./fields";

interface Props {
	projectId: number;
	feature: GeoFeature;
	map: GeoMap;
	onChange: () => void;
}

type NetworkStatus = "supply" | "connected" | "excluded";

function round2(value: number): number {
	return Math.round(value * 100) / 100;
}

function networkStatusOf(data: BuildingData): NetworkStatus {
	if (data.isSupplyCenter) return "supply";
	if (data.isIncluded) return "connected";
	return "excluded";
}

function networkStatusToProps(status: NetworkStatus): BuildingProps {
	switch (status) {
		case "supply":
			return { isHeated: false, isIncluded: false, isSupplyCenter: true };
		case "connected":
			return { isIncluded: true, isSupplyCenter: false };
		case "excluded":
			return { isIncluded: false, isSupplyCenter: false };
	}
}

/// If the feature `next` was set as new supply center, we need to reset
/// a possible other feature that was set as supply center before.
function unsetSupplyCenter(next: GeoFeature, map: GeoMap) {
	if (!map?.features) return;
	for (const f of map.features) {
		if (f === next) continue;
		if (!isBuilding(f)) continue;
		const props = f.properties;
		if (props?.isSupplyCenter) {
			props.isSupplyCenter = false;
			if (props.isHeated) {
				props.isIncluded = true;
			}
		}
	}
}

export const BuildingPanel = ({ projectId, feature, map, onChange }: Props) => {
	const [data, setData] = useState<BuildingData>(BuildingData.of(feature));
	const [isEstimating, setEstimating] = useState(false);
	const [estimateError, setEstimateError] = useState<string | null>(null);
	useEffect(() => {
		setData(BuildingData.of(feature));
	}, [feature]);

	const applyChange = (base: BuildingData, change: BuildingProps) => {
		if (change.isSupplyCenter === true) {
			unsetSupplyCenter(feature, map);
		}

		const next = base.copyWith(change);
		next.applyOn(feature);
		setData(next);
		onChange();
		setEstimateError(null);
	};

	const put = (change: BuildingProps) => {
		applyChange(data, change);
	};

	const onEstimate = async () => {
		if (!data.isHeated || isEstimating) {
			return;
		}
		setEstimating(true);
		setEstimateError(null);
		const res = await api.estimateBuilding(projectId, feature);
		if (res.isOk) {
			applyChange(BuildingData.of(feature), {
				heatDemand: res.value.heatDemand,
				peakLoad: res.value.peakLoad,
			});
		} else {
			setEstimateError(`Failed to estimate heat demand: ${res.error}`);
		}
		setEstimating(false);
	};

	return (
		<div className="card">
			<div className="card-body">
				<StringField
					label="Building"
					value={data.name}
					onChange={value => put({ name: value })}
				/>

				<SelectField
					label="Network status"
					value={networkStatusOf(data)}
					options={[
						{ value: "supply", label: "Supply hub" },
						{ value: "connected", label: "Connected" },
						{ value: "excluded", label: "Excluded" },
					]}
					onChange={value => put(networkStatusToProps(value as NetworkStatus))}
				/>

				<CheckboxField
					label="Is heated"
					checked={data.isHeated}
					onChange={checked => put({ isHeated: checked })}
				/>

				<NumberField
					label="Height (m)"
					value={round2(data.height)}
					step="0.1"
					onChange={value => put({ height: value })}
				/>

				<NumberField
					label="Ground Area (m²)"
					value={round2(data.groundArea)}
					step="0.1"
					onChange={value => put({ groundArea: value })}
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
						{
							value: BuildingType.MULTI_FAMILY_SMALL,
							label: "Multi Family Small",
						},
						{
							value: BuildingType.MULTI_FAMILY_MEDIUM,
							label: "Multi Family Medium",
						},
						{
							value: BuildingType.MULTI_FAMILY_LARGE,
							label: "Multi Family Large",
						},
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
						{
							value: ConstructionAge.UNKNOWN,
							label: constructionAgeToString(ConstructionAge.UNKNOWN),
						},
						{
							value: ConstructionAge.AGE_1900_1919,
							label: constructionAgeToString(ConstructionAge.AGE_1900_1919),
						},
						{
							value: ConstructionAge.AGE_1919_1948,
							label: constructionAgeToString(ConstructionAge.AGE_1919_1948),
						},
						{
							value: ConstructionAge.AGE_1949_1978,
							label: constructionAgeToString(ConstructionAge.AGE_1949_1978),
						},
						{
							value: ConstructionAge.AGE_1979_1995,
							label: constructionAgeToString(ConstructionAge.AGE_1979_1995),
						},
						{
							value: ConstructionAge.AGE_1995_2009,
							label: constructionAgeToString(ConstructionAge.AGE_1995_2009),
						},
						{
							value: ConstructionAge.AGE_2010_2030,
							label: constructionAgeToString(ConstructionAge.AGE_2010_2030),
						},
					]}
					onChange={value => put({ constructionAge: value })}
				/>

				{data.isHeated && (
					<>
						<NumberField
							label="Heat demand (kWh)"
							value={round2(data.heatDemand)}
							step="0.1"
							onChange={value => put({ heatDemand: value })}
						/>

						<NumberField
							label="Peak load (kW)"
							value={round2(data.peakLoad)}
							step="0.1"
							onChange={value => put({ peakLoad: value })}
						/>
					</>
				)}

				{estimateError && (
					<div className="alert alert-danger py-2 mb-2" role="alert">
						<small>{estimateError}</small>
					</div>
				)}

				{data.isHeated && (
					<div className="row mb-1">
						<div className="col-sm-5" />
						<div className="col-sm-7">
							<button
								className="btn btn-outline-primary"
								onClick={onEstimate}
								disabled={!data.isHeated || isEstimating}
								title="Estimate heat demand and peak load"
								style={{ width: "100%" }}>
								{isEstimating ? " Estimating..." : " Estimate heat demand"}
							</button>
						</div>
					</div>
				)}

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
