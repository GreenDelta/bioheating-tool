package com.greendelta.bioheating.examples;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.greendelta.bioheating.predict.BoostPredictor;
import com.greendelta.bioheating.predict.ModelValidator;

public class ModelValidationExample {

	public static void main(String[] args) {
		var model = BoostPredictor.getDefault().orElseThrow().booster();

		var dataDir = new File("./model-training/data");

		var selfCheck = ModelValidator.validate(
			model,
			new File(dataDir, "training-data.csv")
		).orElseThrow();
		writeResults(selfCheck, new File(dataDir, "self-check.txt"));

		var validationCheck = ModelValidator.validate(
			model,
			new File(dataDir, "validation-data.csv")
		).orElseThrow();
		writeResults(validationCheck, new File(dataDir, "validation-check.txt"));

		System.out.println("All done!");
	}

	private static void writeResults(
		ModelValidator.ValidationResult result,
		File outputFile
	) {
		try (var w = new PrintWriter(new FileWriter(outputFile))) {
			for (var pair : result.pairs()) {
				w.printf("%f\t%f%n", pair.actual(), pair.predicted());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
