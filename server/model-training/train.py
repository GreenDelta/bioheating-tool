#!/usr/bin/env python3
"""
Training script for XGBoost heat demand prediction model.

This script trains an XGBoost model using building data to predict heat demand.
It follows the exact same training logic as Training.java in the Java codebase.
"""

import os
import csv
import numpy as np
import xgboost as xgb


def get_building_type_factor(code):
    """Returns the building type factor based on building type code."""
    mapping = {
        1: 0.65,  # HIGH_RISE
        2: 0.8,  # MULTI_FAMILY_SMALL
        3: 0.75,  # MULTI_FAMILY_MEDIUM
        4: 0.7,  # MULTI_FAMILY_LARGE
        5: 0.8,  # BUILDING_PART
        6: 1.0,  # SINGLE_FAMILY
        7: 0.9,  # END_TERRACE
        8: 0.8,  # MID_TERRACE
        9: 0.88,  # HOUSE_GROUP
        0: 0.8,  # OTHER
    }
    return mapping.get(code, 0.8)


def get_climate_region_factor(code):
    """Returns the climate region factor based on climate region code."""
    mapping = {
        1: 0.85,
        2: 1.08,
        3: 0.83,
        4: 0.84,
        5: 1.01,
        6: 0.8,
        7: 1.06,
        8: 0.84,
        9: 0.89,
        10: 1.06,
        11: 1.16,
        12: 0.91,
        13: 1.15,
        14: 1.19,
        15: 0.89,
    }
    return mapping.get(code, 0.91)


def get_average_heat_demand(age_code):
    """Returns the average heat demand in kWh/m²/year based on construction age."""
    mapping = {
        0: 130.0,  # UNKNOWN
        1: 180.0,  # AGE_1900_1919
        2: 190.0,  # AGE_1919_1948
        3: 210.0,  # AGE_1949_1978
        4: 150.0,  # AGE_1979_1995
        5: 80.0,  # AGE_1995_2009
        6: 50.0,  # AGE_2010_2030
    }
    return mapping.get(age_code, 130.0)


def read_csv_data(csv_file):
    """
    Reads CSV data and returns features and labels.

    CSV columns:
    0: buildingId
    1: height (meters)
    2: storeys
    3: groundArea (square meters)
    4: buildingTypeCode
    5: climateRegionCode
    6: constructionAgeCode
    7: roofTypeFactor
    8: heatDemand (kWh)

    Returns:
        tuple: (features, labels, building_ids) where features is a 2D array with 7 features per row
    """
    features = []
    labels = []
    building_ids = []

    with open(csv_file, "r") as f:
        reader = csv.reader(f)
        next(reader)  # Skip header

        for row in reader:
            if len(row) < 9:
                continue

            # Parse CSV columns
            building_id = row[0].strip().strip('"')
            height = float(row[1])
            storeys = int(row[2])
            ground_area = float(row[3])
            building_type_code = int(row[4])
            climate_region_code = int(row[5])
            construction_age_code = int(row[6])
            roof_type_factor = float(row[7])
            heat_demand = float(row[8])

            # Encode features (same order as CsvEncoder.java)
            # The 7 features are:
            # 0: height
            # 1: storeys
            # 2: groundArea
            # 3: buildingTypeFactor
            # 4: climateRegionFactor
            # 5: averageHeatDemand
            # 6: roofTypeFactor
            feature_vector = [
                height,
                storeys,
                ground_area,
                get_building_type_factor(building_type_code),
                get_climate_region_factor(climate_region_code),
                get_average_heat_demand(construction_age_code),
                roof_type_factor,
            ]

            features.append(feature_vector)
            labels.append(heat_demand)
            building_ids.append(building_id)

    return (
        np.array(features, dtype=np.float32),
        np.array(labels, dtype=np.float32),
        building_ids,
    )


def train_model(training_file, output_model_file):
    """
    Trains an XGBoost model using the training data.

    Args:
        training_file: Path to CSV file with training data
        output_model_file: Path where the trained model will be saved
    """
    print(f"Reading training data from: {training_file}")
    features, labels, _ = read_csv_data(training_file)
    print(f"Loaded {len(features)} training samples with {features.shape[1]} features")

    # Create DMatrix (XGBoost's internal data structure)
    dtrain = xgb.DMatrix(features, label=labels)

    # Configure training parameters (same as Training.java)
    params = {
        "objective": "reg:squarederror",  # Regression task
        "tree_method": "hist",
        "reg_alpha": 0.1,  # Note: Java code has typo "reg_aplha", using correct name
        "eta": 0.5,  # Learning rate
        "max_depth": 6,  # Maximum tree depth
    }

    # Train the model
    print("Training model...")
    num_rounds = 1000
    model = xgb.train(params, dtrain, num_rounds)

    # Save the model
    print(f"Saving model to: {output_model_file}")
    os.makedirs(os.path.dirname(output_model_file), exist_ok=True)
    model.save_model(output_model_file)
    print("Model saved successfully!")

    return model


def validate_model(model, validation_file, output_file):
    """
    Validates the model on validation data and saves predictions.

    Args:
        model: Trained XGBoost model
        validation_file: Path to CSV file with validation data
        output_file: Path where validation results will be saved
    """
    print(f"\nReading validation data from: {validation_file}")
    features, labels, building_ids = read_csv_data(validation_file)
    print(f"Loaded {len(features)} validation samples")

    # Create DMatrix for prediction (without labels)
    dvalid = xgb.DMatrix(features)

    # Make predictions
    print("Making predictions...")
    predictions = model.predict(dvalid)

    # Calculate metrics
    errors = labels - predictions
    squared_errors = errors**2
    absolute_errors = np.abs(errors)

    rmse = np.sqrt(np.mean(squared_errors))
    mae = np.mean(absolute_errors)
    mse = np.mean(squared_errors)

    # Calculate R²
    mean_actual = np.mean(labels)
    ss_total = np.sum((labels - mean_actual) ** 2)
    ss_residual = np.sum(squared_errors)
    r2 = 1.0 - (ss_residual / ss_total) if ss_total != 0 else 0.0

    print("\nValidation Metrics:")
    print(f"  RMSE: {rmse:.2f}")
    print(f"  MAE:  {mae:.2f}")
    print(f"  MSE:  {mse:.2f}")
    print(f"  R²:   {r2:.4f}")

    # Save predictions to file
    print(f"\nSaving validation results to: {output_file}")
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, "w") as f:
        f.write("expected\tpredicted\n")
        for expected, predicted in zip(labels, predictions):
            f.write(f"{expected}\t{predicted}\n")
    print("Validation results saved!")


def main():
    # Get script directory
    script_dir = os.path.dirname(os.path.abspath(__file__))

    # Define file paths
    training_file = os.path.join(script_dir, "data", "training-data.csv")
    validation_file = os.path.join(script_dir, "data", "validation-data.csv")
    validation_output_file = os.path.join(script_dir, "data", "validation-check.txt")

    # Model output path (in Java project resources)
    model_output_file = os.path.join(
        script_dir,
        "..",
        "src",
        "main",
        "resources",
        "com",
        "greendelta",
        "bioheating",
        "predict",
        "model.bin",
    )

    # Train the model
    model = train_model(training_file, model_output_file)

    # Validate the model
    validate_model(model, validation_file, validation_output_file)

    print("\n✓ Training and validation completed successfully!")


if __name__ == "__main__":
    main()
