package com.greendelta.bioheating.examples;

import java.io.File;
import java.io.FileOutputStream;

import com.greendelta.bioheating.predict.Training;

public class ModelTrainingExample {

	public static void main(String[] args) {
		try (var out = new FileOutputStream(new File("./target/model.json"))) {
			System.out.println("Read training data & train model ...");
			var path = "C:/Users/ms/Projects/Bioheating/data/ai-training-data/"
				+ "training-data.csv";
			var model = Training.trainFrom(new File(path)).orElseThrow();
			System.out.println("Saving model ...");
			model.saveModel(out);
			System.out.println("All done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
