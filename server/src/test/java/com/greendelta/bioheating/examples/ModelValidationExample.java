package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.predict.BoostPredictor;
import com.greendelta.bioheating.predict.ModelValidator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ModelValidationExample {

	public static void main(String[] args) {
		var predictor = BoostPredictor.getDefault().orElseThrow();
		var dataDir = new File("./model-training/data");

		validate(predictor, dataDir, "training-data.csv", "self-check.txt");
		validate(
			predictor,
			dataDir,
			"validation-data.csv",
			"validation-check.txt"
		);

		System.out.println("All done!");
	}

	private static void validate(
		BoostPredictor predictor,
		File dataDir,
		String dataFile,
		String checkFile
	) {
		var result = ModelValidator.validate(
			predictor,
			new File(dataDir, dataFile)
		).orElseThrow();
		writeCheckFile(result, new File(dataDir, checkFile));

		System.out.println(dataFile);
		System.out.println("  heat demand: " + result.heatDemand().metrics());
		System.out.println("  peak load:   " + result.peakLoad().metrics());
	}

	private static void writeCheckFile(
		ModelValidator.ValidationResult result,
		File file
	) {
		try (var w = new PrintWriter(new FileWriter(file))) {
			var heat = result.heatDemand().values();
			var peak = result.peakLoad().values();
			for (var i = 0; i < heat.size(); i++) {
				w.printf(
					"%f\t%f\t%f\t%f%n",
					heat.get(i).expected(),
					heat.get(i).predicted(),
					peak.get(i).expected(),
					peak.get(i).predicted()
				);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
