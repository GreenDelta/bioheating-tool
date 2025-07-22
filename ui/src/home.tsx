import React from 'react';

export const HomePage = () => {
	return <>

		<div className="container-fluid">
			<div className="row">
				<div className="col-md-7">

					<div className="my-3">
						<strong>Welcome to BIOHEATING</strong>
					</div>
					<p>
						BIOHEATING is a forward-thinking project aimed at making the planning of
						sustainable heating networks more accessible, accurate, and efficient.
						It combines the power of geospatial data, machine learning, and a
						user-friendly interface to support the design of modern district heating
						systems based on renewable bioenergy sources.
					</p>

					<p>
						At the heart of the BIOHEATING platform is a tool that automatically
						estimates the heating demand of individual buildings. This is achieved
						through advanced machine learning algorithms trained on comprehensive
						GIS (Geographic Information System) data, allowing users to quickly
						assess energy needs across entire neighborhoods or municipalities.
					</p>

					<p>
						Once the demand is calculated, the tool enables users to design a
						complete heating network layout. This includes determining optimal
						pipe routing, calculating pipe diameters and other key technical
						specifications. All of this takes place within a streamlined and
						visually intuitive interface that guides users through each stage
						of the planning process.
					</p>

					<p>
						A key strength of BIOHEATING is its integration with existing energy
						planning tools. Projects developed within the platform can be exported
						directly to Sophena, a well-established software used for detailed
						analysis and simulation of heating networks. This ensures that the
						preliminary designs created in BIOHEATING can seamlessly transition
						into technical and economic feasibility studies.
					</p>

					<p>
						By bringing together intelligent demand estimation, network planning,
						and interoperability with established tools, BIOHEATING supports a
						more sustainable, data-driven approach to heating infrastructure
						development. Whether you are a planner, engineer, or decision-maker,
						BIOHEATING helps turn ideas into energy-efficient, climate-conscious
						solutions.
					</p>
				</div>

				<div className="col-md-5">
					<img
						src="/img/home.png"
						alt="BIOHEATING"
						className="img-fluid rounded shadow mb-3"
					/>
				</div>
			</div>
		</div>
	</>
}
