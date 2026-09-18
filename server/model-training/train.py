"""Train the multi-output model for heat demand and peak load prediction.

The model is trained on the CSV files that are produced from the simulation
output Excel files with ``xls_simout_to_csv.py``.  Such a CSV contains six
features and two targets:

    +-----+--------------------------+--------------------------------------+
    | Idx | Feature                  | Meaning                              |
    +-----+--------------------------+--------------------------------------+
    | 0   | ground area [m2]         | ground surface area of the building  |
    | 1   | height [m]               | building height                      |
    | 2   | weather station [code]   | weather station / climate region     |
    | 3   | construction year [code] | construction age code (1..6)         |
    | 4   | roof type [1|0]          | 1 = flat roof, 0 = pitched roof      |
    | 5   | building type [code]     | building classification (1..10)      |
    +-----+--------------------------+--------------------------------------+
    | 6   | heat demand [kWh]        | target: annual heat demand           |
    | 7   | peak load [kW]           | target: maximum heat load            |
    +-----+--------------------------+--------------------------------------+

All raw values and codes are used directly, no factor mappings are applied.
Both targets are predicted by a single model, i.e. the model has two outputs.
XGBoost handles this as a multi-output regression: the label matrix has two
columns and ``multi_strategy`` controls how the trees are built (see PARAMS).
The trained booster is saved as a single ``model.ubj`` file.

The script trains on ``data/training-data.csv`` and writes the model to
``../src/main/resources/com/greendelta/bioheating/predict/model.ubj``.  It then
writes two check files for GnuPlot:

    data/self-check.txt        predictions on the training data
    data/validation-check.txt  predictions on the validation data

Each line of a check file contains four tab-separated values:
``heat_expected  heat_predicted  peak_expected  peak_predicted``.
"""

import csv
from pathlib import Path

import numpy as np
import xgboost as xgb

from xls_simout_to_csv import CSV_HEADER

# Column indices of the training CSV, see xls_simout_to_csv.py.
COL_GROUND_AREA = 0
COL_HEIGHT = 1
COL_WEATHER_STATION = 2
COL_CONSTRUCTION_YEAR = 3
COL_ROOF_TYPE = 4
COL_BUILDING_TYPE = 5
COL_HEAT_DEMAND = 6
COL_PEAK_LOAD = 7
COL_COUNT = 8

# Number of model inputs and outputs and the names of the two targets.
FEATURE_COUNT = 6
TARGET_NAMES = ["heat demand", "peak load"]

# XGBoost training parameters.  ``multi_output_tree`` builds a single tree per
# boosting round that predicts both targets at once, which lets the model use
# the correlation between heat demand and peak load.
PARAMS = {
    "objective": "reg:squarederror",
    "tree_method": "hist",
    "multi_strategy": "multi_output_tree",
    "reg_alpha": 0.1,
    "eta": 0.5,  # learning rate
    "max_depth": 6,
}
NUM_ROUNDS = 1000


def read_csv_data(csv_file: Path) -> tuple[np.ndarray, np.ndarray]:
    """Read the features and the two target columns from a training CSV."""
    features = []
    labels = []

    with open(csv_file, "r", encoding="utf-8", newline="") as f:
        reader = csv.reader(f)
        header = next(reader, None)
        if header != CSV_HEADER:
            raise ValueError(
                f"{csv_file.name} has an unexpected header {header}; "
                "convert the simulation output with xls_simout_to_csv.py"
            )

        for line, row in enumerate(reader, start=2):
            if not row:
                continue
            if len(row) < COL_COUNT:
                raise ValueError(
                    f"{csv_file.name}:{line} has {len(row)} columns, "
                    f"expected {COL_COUNT}"
                )

            features.append(
                [
                    float(row[COL_GROUND_AREA]),
                    float(row[COL_HEIGHT]),
                    float(row[COL_WEATHER_STATION]),
                    float(row[COL_CONSTRUCTION_YEAR]),
                    float(row[COL_ROOF_TYPE]),
                    float(row[COL_BUILDING_TYPE]),
                ]
            )
            labels.append([float(row[COL_HEAT_DEMAND]), float(row[COL_PEAK_LOAD])])

    if not features:
        raise ValueError(f"{csv_file.name} contains no data rows")

    return (
        np.array(features, dtype=np.float32),
        np.array(labels, dtype=np.float32),
    )


def train_model(training_file: Path, output_model_file: Path) -> xgb.Booster:
    """Train the multi-output model and save it to ``output_model_file``."""
    print(f"Read training data from: {training_file.name}")
    features, labels = read_csv_data(training_file)
    dtrain = xgb.DMatrix(features, label=labels)

    print(f"Train model with {FEATURE_COUNT} features and {len(TARGET_NAMES)} targets...")
    model = xgb.train(PARAMS, dtrain, NUM_ROUNDS)

    print(f"Saving model to: {output_model_file.name}")
    output_model_file.parent.mkdir(exist_ok=True, parents=True)
    # Save in binary JSON (UBJ) format for better cross-platform compatibility
    model.save_model(output_model_file)
    return model


def validate_model(model: xgb.Booster, validation_file: Path, out_file: Path) -> None:
    """Write the expected and predicted values of both targets to ``out_file``."""
    print(f"Validate model with {validation_file.name}")
    features, labels = read_csv_data(validation_file)
    predictions = model.predict(xgb.DMatrix(features))

    out_file.parent.mkdir(exist_ok=True, parents=True)
    with open(out_file, "w", encoding="utf-8") as f:
        for expected, predicted in zip(labels, predictions):
            f.write(
                f"{expected[0]}\t{predicted[0]}\t"
                f"{expected[1]}\t{predicted[1]}\n"
            )

    for i, name in enumerate(TARGET_NAMES):
        error = labels[:, i] - predictions[:, i]
        rmse = float(np.sqrt(np.mean(error**2)))
        print(f"  {name}: RMSE = {rmse:.2f}")


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
