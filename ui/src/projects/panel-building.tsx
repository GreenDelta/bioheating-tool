import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import * as api from "../api";
import { errorText } from "../i18n";
import {
	GeoFeature,
	GeoMap,
	BuildingType,
	ConstructionAge,
	constructionAgeToString,
	RoofType,
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
	const { t } = useTranslation();
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
			setEstimateError(errorText(res.error));
		}
		setEstimating(false);
	};

	return (
		<div className="card">
			<div className="card-body">
				<StringField
					label={t("building.name")}
					value={data.name}
					onChange={value => put({ name: value })}
				/>

				<SelectField
					label={t("building.networkStatus")}
					value={networkStatusOf(data)}
					options={[
						{ value: "supply", label: t("building.status.supply") },
						{ value: "connected", label: t("building.status.connected") },
						{ value: "excluded", label: t("building.status.excluded") },
					]}
					onChange={value => put(networkStatusToProps(value as NetworkStatus))}
				/>

				<CheckboxField
					label={t("building.isHeated")}
					checked={data.isHeated}
					onChange={checked => put({ isHeated: checked })}
				/>

				<NumberField
					label={t("building.height")}
					value={round2(data.height)}
					step="0.1"
					onChange={value => put({ height: value })}
				/>

				<NumberField
					label={t("building.groundArea")}
					value={round2(data.groundArea)}
					step="0.1"
					onChange={value => put({ groundArea: value })}
				/>

				<SelectField
					label={t("building.roofType")}
					value={data.roofType}
					options={Object.values(RoofType).map(value => ({
						value,
						label: t(`building.roofTypes.${value}`),
					}))}
					onChange={value => put({ roofType: value })}
				/>

				<StringField
					label={t("building.function")}
					value={data.functionLabel}
					onChange={value => put({ functionLabel: value })}
				/>

				<SelectField
					label={t("building.type")}
					value={data.type}
					options={Object.values(BuildingType).map(value => ({
						value,
						label: t(`building.types.${value}`),
					}))}
					onChange={value => put({ type: value })}
				/>

				<SelectField
					label={t("building.constructionAge")}
					value={data.constructionAge}
					options={Object.values(ConstructionAge).map(value => ({
						value,
						label: constructionAgeToString(value),
					}))}
					onChange={value => put({ constructionAge: value })}
				/>

				{data.isHeated && (
					<>
						<NumberField
							label={t("building.warmWaterFraction")}
							value={round2(data.warmWaterFraction)}
							step="0.1"
							onChange={value => put({ warmWaterFraction: value })}
						/>

						<NumberField
							label={t("building.heatDemand")}
							value={round2(data.heatDemand)}
							step="0.1"
							onChange={value => put({ heatDemand: value })}
						/>

						<NumberField
							label={t("building.peakLoad")}
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
								title={t("building.estimateTitle")}
								style={{ width: "100%" }}>
								{isEstimating ? t("building.estimating") : t("building.estimate")}
							</button>
						</div>
					</div>
				)}

				<hr />
				<h6>{t("building.address")}</h6>

				<StringField
					label={t("building.country")}
					value={data.country}
					onChange={value => put({ country: value })}
				/>

				<StringField
					label={t("building.locality")}
					value={data.locality}
					onChange={value => put({ locality: value })}
				/>

				<StringField
					label={t("building.postalCode")}
					value={data.postalCode}
					onChange={value => put({ postalCode: value })}
				/>

				<StringField
					label={t("building.street")}
					value={data.street}
					onChange={value => put({ street: value })}
				/>

				<StringField
					label={t("building.streetNumber")}
					value={data.streetNumber}
					onChange={value => put({ streetNumber: value })}
				/>
			</div>
		</div>
	);
};
