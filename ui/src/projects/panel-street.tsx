import React, { useState, useEffect } from "react";
import { GeoFeature } from "../model";
import { StreetData, StreetProps } from "./panel-data";
import { StringField, CheckboxField } from "./fields";

interface Props {
	feature: GeoFeature;
	onChange: () => void;
}

export const StreetPanel = ({ feature, onChange }: Props) => {
	const [data, setData] = useState<StreetData>(StreetData.of(feature));
	useEffect(() => {
		setData(StreetData.of(feature));
	}, [feature]);

	const put = (change: StreetProps) => {
		const next = data.copyWith(change);
		next.applyOn(feature);
		setData(next);
		onChange();
	};

	return (
		<div className="card">
			<div className="card-body">
				<h6>Street Information</h6>

				<StringField
					label="Street Name"
					value={data.name}
					onChange={value => put({ name: value })}
				/>

				<CheckboxField
					label="Exclude from solution"
					checked={data.isExcluded}
					onChange={checked => put({ isExcluded: checked })}
				/>
			</div>
		</div>
	);
};
