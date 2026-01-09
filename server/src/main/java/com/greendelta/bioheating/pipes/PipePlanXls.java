package com.greendelta.bioheating.pipes;

import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.commons.Res;

import com.greendelta.bioheating.graph.NetworkTree;
import com.greendelta.bioheating.graph.NetworkTree.Junction;

/// Exports the pipe plan to a flat Excel file.
public class PipePlanXls {

	private final NetworkTree tree;
	private final PipePlan plan;
	private final Workbook workbook;
	private final Sheet sheet;
	private final CellStyle headerStyle;
	private final CellStyle buildingStyle;
	private final CellStyle streetStyle;
	private int rowIndex;

	private PipePlanXls(NetworkTree tree, PipePlan plan) {
		this.tree = tree;
		this.plan = plan;
		this.workbook = new XSSFWorkbook();
		this.sheet = workbook.createSheet("Pipe Plan");
		this.headerStyle = createHeaderStyle();
		this.buildingStyle = createBuildingStyle();
		this.streetStyle = createStreetStyle();
		this.rowIndex = 0;
	}

	public static Res<byte[]> create(PipeConfig config, NetworkTree tree) {
		if (tree == null || tree.root() == null)
			return Res.error("No tree provided");
		var planRes = PipePlan.of(config, tree);
		if (planRes.isError())
			return planRes.castError();
		return new PipePlanXls(tree, planRes.value()).build();
	}

	private Res<byte[]> build() {
		try {
			writeHeader();
			writeNode(tree.root(), 0);
			for (int i = 0; i < 8; i++) {
				sheet.autoSizeColumn(i);
			}
			try (var out = new ByteArrayOutputStream()) {
				workbook.write(out);
				workbook.close();
				return Res.ok(out.toByteArray());
			}
		} catch (Exception e) {
			return Res.error("Failed to create Excel file", e);
		}
	}

	private void writeHeader() {
		var row = sheet.createRow(rowIndex++);
		var headers = new String[]{
			"Level", "Node ID", "Type", "Building","Peak Load (kW)",
			"Pipe Diameter (mm)", "Pipe Length (m)"
		};

		for (int i = 0; i < headers.length; i++) {
			var cell = row.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}
	}

	private void writeNode(Junction node, int level) {
		if (node == null)
			return;

		var row = sheet.createRow(rowIndex++);
		var style = node.isBuilding() ? buildingStyle : streetStyle;

		// Level (indentation)
		var levelCell = row.createCell(0);
		levelCell.setCellValue(level);
		levelCell.setCellStyle(style);

		// Node ID
		var idCell = row.createCell(1);
		idCell.setCellValue(node.id());
		idCell.setCellStyle(style);

		// Type
		var typeCell = row.createCell(2);
		typeCell.setCellValue(node.isBuilding() ? "Building" : "Street Node");
		typeCell.setCellStyle(style);

		// Building name/info
		var buildingCell = row.createCell(3);
		if (node.building() != null) {
			var building = node.building();
			var name = building.name() != null
				? building.name()
				: "Building-" + building.id();
			if (building.isSupplyCenter()) {
				name += " (Supply Center)";
			}
			buildingCell.setCellValue(name);
		} else {
			buildingCell.setCellValue("-");
		}
		buildingCell.setCellStyle(style);

		// Peak Load
		var loadCell = row.createCell(4);
		loadCell.setCellValue(Math.round(plan.peakLoadOf(node) * 10.0) / 10.0);
		loadCell.setCellStyle(style);

		// Pipe diameter and length are for incoming segments
		row.createCell(5).setCellStyle(style);
		row.createCell(6).setCellStyle(style);

		// Write child segments
		for (var segment : node.segments()) {
			writeSegment(segment, level + 1);
			writeNode(segment.target(), level + 1);
		}
	}

	private void writeSegment(NetworkTree.Segment segment, int level) {
		var row = sheet.createRow(rowIndex++);
		var style = createSegmentStyle();

		// Level
		var levelCell = row.createCell(0);
		levelCell.setCellValue(level);
		levelCell.setCellStyle(style);

		// Segment ID
		var idCell = row.createCell(1);
		idCell.setCellValue("→ " + segment.id());
		idCell.setCellStyle(style);

		// Type
		var typeCell = row.createCell(2);
		typeCell.setCellValue("Pipe Segment");
		typeCell.setCellStyle(style);

		// Empty building column
		row.createCell(3).setCellStyle(style);

		// Peak Load
		var loadCell = row.createCell(4);
		loadCell.setCellValue(Math.round(plan.peakLoadOf(segment) * 10.0) / 10.0);
		loadCell.setCellStyle(style);

		// Pipe info
		var pipe = plan.pipeOf(segment);
		if (pipe != null) {
			var diameterCell = row.createCell(5);
			diameterCell.setCellValue(Math.round(pipe.innerDiameter()));
			diameterCell.setCellStyle(style);
		} else {
			row.createCell(5).setCellStyle(style);
			row.createCell(7).setCellStyle(style);
		}

		// Length
		var lengthCell = row.createCell(6);
		lengthCell.setCellValue(Math.round(segment.length() * 10.0) / 10.0);
		lengthCell.setCellStyle(style);
	}

	private CellStyle createHeaderStyle() {
		var style = workbook.createCellStyle();
		var font = workbook.createFont();
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(font);
		style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}

	private CellStyle createBuildingStyle() {
		var style = workbook.createCellStyle();
		var font = workbook.createFont();
		font.setBold(true);
		style.setFont(font);
		style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}

	private CellStyle createStreetStyle() {
		var style = workbook.createCellStyle();
		style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}

	private CellStyle createSegmentStyle() {
		var style = workbook.createCellStyle();
		var font = workbook.createFont();
		font.setItalic(true);
		font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setFont(font);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}
}
