package com.greendelta.bioheating.citygml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import org.citygml4j.core.model.core.CityModel;
import org.citygml4j.xml.CityGMLContext;
import org.citygml4j.xml.reader.CityGMLReadException;
import org.citygml4j.xml.reader.CityGMLReader;
import org.xmlobjects.gml.model.basictypes.Code;
import org.xmlobjects.gml.model.deprecated.StringOrRef;

import com.greendelta.bioheating.util.Res;

class CityGML {

	private CityGML() {
	}

	static Res<CityModel> readModel(File file) {
		try (var stream = new FileInputStream(file);
				 var buffer = new BufferedInputStream(stream)) {
			return readModel(buffer);
		} catch (Exception e) {
			return Res.error("failed to read CityModel from " + file.getName(), e);
		}
	}

	static Res<CityModel> readModel(Reader reader) {
		if (reader == null)
			return Res.error("Reader is null");
		try {
			var cityGMLReader = CityGMLContext.newInstance()
				.createCityGMLInputFactory()
				.createCityGMLReader(reader);
			return findModel(cityGMLReader);
		} catch (Exception e) {
			return Res.error("failed to read CityModel", e);
		}
	}

	static Res<CityModel> readModel(InputStream stream) {
		if (stream == null)
			return Res.error("input stream is null");
		try {
			var reader = CityGMLContext.newInstance()
				.createCityGMLInputFactory()
				.createCityGMLReader(stream);
			return findModel(reader);
		} catch (Exception e) {
			return Res.error("failed to read CityModel", e);
		}
	}

	private static Res<CityModel> findModel(CityGMLReader reader)
		throws CityGMLReadException {
		try (reader) {
			if (!reader.hasNext())
				return Res.error("no CityModel found");
			var next = reader.next();
			if (!(next instanceof CityModel model))
				return Res.error("no CityModel found");
			return Res.of(model);
		}
	}

	static String stringOf(Code code) {
		return code != null ? code.getValue() : null;
	}

	static String firstStringOf(List<Code> codes) {
		return codes != null && !codes.isEmpty()
			? stringOf(codes.getFirst())
			: null;
	}

	static String stringOf(StringOrRef s) {
		return s != null
			? s.getValue()
			: null;
	}
}
