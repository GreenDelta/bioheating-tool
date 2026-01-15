package com.greendelta.bioheating.model.client;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.greendelta.bioheating.model.Solution;

public record ClientSolution(
	long id,
	String name,
	long projectId,
	String calculatedAt,
	double heatDemand,
	double length
) {

	private static final DateTimeFormatter FORMATTER =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static ClientSolution of(Solution s) {
		var p = s.project();
		var name = p != null ? p.name() : "No project";
		long projectId = p != null ? p.id() : -1;

		var timeStamp = "";
		if (s.calculatedAt() > 0) {
			var dateTime = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(s.calculatedAt()),
				ZoneId.systemDefault()
			);
			timeStamp = dateTime.format(FORMATTER);
		}
		return new ClientSolution(
			s.id(), name, projectId, timeStamp, s.heatDemand(), s.length());
	}

}
