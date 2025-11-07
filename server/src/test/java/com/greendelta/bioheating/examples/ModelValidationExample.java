package com.greendelta.bioheating.examples;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import com.greendelta.bioheating.predict.ModelValidator;

import ml.dmlc.xgboost4j.java.XGBoost;

public class ModelValidationExample {

	public static void main(String[] args) {
		try {
			System.out.println("Loading model ...");
			var modelFile = new File("./target/model.bin");
			var model = XGBoost.loadModel(modelFile.getAbsolutePath());

			System.out.println("Loading validation data ...");
			var dataPath = "C:/Users/ms/Projects/Bioheating/data/ai-training-data/"
				+ "training-data.csv";
			var dataFile = new File(dataPath);

			System.out.println("Validating model ...");
			var result = ModelValidator.validate(model, dataFile).orElseThrow();

			System.out.println("Validation metrics:");
			System.out.println(result.metrics());

			System.out.println("Writing validation results to CSV ...");
			var outputFile = new File("./target/model-validation.csv");
			try (var writer = new PrintWriter(new FileWriter(outputFile))) {
				// Write header
				writer.println("buildingId,actual,predicted,error,absoluteError");

				// Write each validation pair
				for (var pair : result.pairs()) {
					writer.printf("%s,%.2f,%.2f,%.2f,%.2f%n",
						pair.buildingId(),
						pair.actual(),
						pair.predicted(),
						pair.error(),
						pair.absoluteError()
					);
				}
			}

			System.out.println("Validation complete!");
			System.out.println("Results written to: " + outputFile.getAbsolutePath());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
