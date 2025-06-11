import React, { useState, useEffect } from 'react';
import { Link, useLoaderData } from 'react-router-dom';
import { Project } from '../model';
import { Map } from './map';
import * as api from '../api';

export const ProjectDetail = () => {

	const res: api.Res<Project> = useLoaderData();
	if (res.isErr) {
		return <div style={{ color: 'red' }}>Error: {res.error}</div>;
	}
	const project = res.value;

	return (
		<div>

			<h2>Project: {project.name}</h2>

			{project.description && (
				<div style={{
					backgroundColor: '#f8f9fa',
					padding: '16px',
					borderRadius: '8px',
					marginTop: '16px'
				}}>
					<h3>Description</h3>
					<p style={{ margin: 0 }}>{project.description}</p>
				</div>
			)}

			<div style={{ marginTop: '32px' }}>
				<h3>Project Map</h3>
				<div style={{
					border: '1px solid #ddd',
					borderRadius: '8px',
					overflow: 'hidden',
					marginTop: '16px'
				}}>
					<Map data={project.map} />
				</div>
			</div>
			<div style={{ marginTop: '32px' }}>
				<h3>Project Details</h3>
				<p><strong>ID:</strong> {project.id}</p>
				<p><strong>Name:</strong> {project.name}</p>
			</div>
		</div>
	);
};
