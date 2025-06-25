import React from 'react';

export const StringField = ({ label, value, onChange }: {
	label: string;
	value: string;
	onChange: (value: string) => void;
}) => (
	<div className="row mb-1">
		<label className="col-sm-4 col-form-label">{label}</label>
		<div className="col-sm-8">
			<input
				className="form-control"
				value={value}
				onChange={e => onChange(e.target.value)}
			/>
		</div>
	</div>
);

export const NumberField = ({ label, value, step, disabled, onChange }: {
	label: string;
	value: number;
	step?: string;
	disabled?: boolean;
	onChange: (value: string) => void;
}) => (
	<div className="row mb-1">
		<label className="col-sm-4 col-form-label">{label}</label>
		<div className="col-sm-8">
			<input
				type="number"
				step={step || "1"}
				className="form-control"
				value={disabled ? "" : value}
				placeholder={disabled ? " - heating disabled - " : ""}
				disabled={disabled || false}
				onChange={e => onChange(e.target.value)}
			/>
		</div>
	</div>
);

export const CheckboxField = ({ label, checked, onChange }: {
	label: string;
	checked: boolean;
	onChange: (checked: boolean) => void;
}) => (
	<div className="row mb-1">
		<label className="col-sm-4 col-form-label">{label}</label>
		<div className="col-sm-8">
			<div className="form-check mt-2">
				<input
					type="checkbox"
					className="form-check-input"
					checked={checked}
					onChange={e => onChange(e.target.checked)}
				/>
			</div>
		</div>
	</div>
);
