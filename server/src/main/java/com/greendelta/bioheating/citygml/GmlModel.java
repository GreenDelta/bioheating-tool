package com.greendelta.bioheating.citygml;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

import org.citygml4j.core.model.core.CityModel;
import org.locationtech.jts.geom.GeometryFactory;

import com.greendelta.bioheating.util.Res;

public record GmlModel(
	String name,
	String description,
	GmlEnvelope envelope
) {

	public static Res<GmlModel> readFrom(File file) {
		var model = CityGML.readModel(file);
		return model.hasError()
			? model.castError()
			: map(model.value());
	}

	public static Res<GmlModel> readFrom(Reader reader) {
		var model = CityGML.readModel(reader);
		return model.hasError()
			? model.castError()
			: map(model.value());
	}

	public static Res<GmlModel> readFrom(InputStream stream) {
		var model = CityGML.readModel(stream);
		return model.hasError()
			? model.castError()
			: map(model.value());
	}

	private static Res<GmlModel> map(CityModel model) {
		if (model == null)
			return Res.error("model is null");
		var factory = new GeometryFactory();
		try {
			var gml = new GmlModel(
				CityGML.firstStringOf(model.getNames()),
				CityGML.stringOf(model.getDescription()),
				GmlEnvelope.of(model, factory).orElse(null)
			);
			return Res.of(gml);
		} catch (Exception e) {
			return Res.error("failed to read model data", e);
		}
	}
}
