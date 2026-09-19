package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.predict.Training;
import java.io.File;

public class ModelTrainingExample {

	public static void main(String[] args) {
		try {
			var dataDir = new File("./model-training/data");
			var models = Training.trainFrom(
				new File(dataDir, "training-data.csv")
			).orElseThrow();

			var modelDir = new File(
				"./src/main/resources/com/greendelta/bioheating/predict"
			);
			Training.save(models, modelDir).orElseThrow();

			System.out.println("All done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
