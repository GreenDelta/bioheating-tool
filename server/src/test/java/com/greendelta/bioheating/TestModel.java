package com.greendelta.bioheating;

import com.greendelta.bioheating.predict.BoostPredictor;
import java.io.File;

/// Provides the trained prediction model for tests. The model is read from
/// `app/model` relative to the server folder (the working directory of Maven),
/// where the training writes it. Tests that need the real model should be
/// skipped when it is not available:
///
/// ```java
/// assumeTrue(TestModel.isAvailable(), TestModel.MISSING);
/// ```
public final class TestModel {

	public static final File DIR = new File("app/model");

	public static final String MISSING =
		"the prediction model is not available in " + DIR.getAbsolutePath();

	private static BoostPredictor predictor;

	private TestModel() {}

	public static boolean isAvailable() {
		return get() != null;
	}

	/// Returns the model or `null` if it is not available.
	public static synchronized BoostPredictor get() {
		if (predictor == null) {
			predictor = BoostPredictor.load(DIR).orElse(() -> null);
		}
		return predictor;
	}
}
