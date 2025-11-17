import csv
import numpy as np
import xgboost as xgb

from pathlib import Path


def get_building_type_factor(code: int) -> float:
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


def get_climate_region_factor(code: int):
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


def read_csv_data(csv_file: Path) -> tuple[np.ndarray, np.ndarray]:
    features = []
    labels = []

    with open(csv_file, "r") as f:
        reader = csv.reader(f)
        next(reader)  # skip header

        for row in reader:
            if len(row) < 9:
                continue

            height = float(row[1])
            storeys = int(row[2])
            ground_area = float(row[3])
            building_type_code = int(row[4])
            climate_region_code = int(row[5])
            construction_age_code = int(row[6])
            roof_type_factor = float(row[7])
            heat_demand = float(row[8])

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

    return (np.array(features, dtype=np.float32), np.array(labels, dtype=np.float32))


def train_model(training_file: Path, output_model_file: Path) -> xgb.Booster:
    print(f"Read training data from: {training_file.name}")
    features, labels = read_csv_data(training_file)
    dtrain = xgb.DMatrix(features, label=labels)

    print("Train model...")
    params = {
        "objective": "reg:squarederror",
        "tree_method": "hist",
        "reg_alpha": 0.1,
        "eta": 0.5,
        "max_depth": 6,
    }
    num_rounds = 1000
    model = xgb.train(params, dtrain, num_rounds)

    print(f"Saving model to: {output_model_file.name}")
    output_model_file.parent.mkdir(exist_ok=True, parents=True)
    # Save in JSON format for better cross-platform compatibility
    model.save_model(output_model_file)
    return model


def validate_model(model: xgb.Booster, validation_file: Path, out_file: Path):
    print(f"Validate model with {validation_file.name}")
    features, labels = read_csv_data(validation_file)
    dvalid = xgb.DMatrix(features)
    predictions = model.predict(dvalid)
    out_file.parent.mkdir(exist_ok=True, parents=True)
    with open(out_file, "w") as f:
        for expected, predicted in zip(labels, predictions):
            f.write(f"{expected}\t{predicted}\n")


def main():
    script_dir = Path(__file__).parent
    data_dir = script_dir / "data"
    model_output_file = (
        script_dir / "../src/main/resources/com/greendelta/bioheating/predict/model.ubj"
    )
    model = train_model(data_dir / "training-data.csv", model_output_file)

    validate_model(
        model, data_dir / "training-data.csv", data_dir / "self-check.txt"
    )
    validate_model(
        model, data_dir / "validation-data.csv", data_dir / "validation-check.txt"
    )

    print("All done!")


if __name__ == "__main__":
    main()
