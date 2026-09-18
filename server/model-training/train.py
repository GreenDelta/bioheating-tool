"""Train the heat demand and peak load prediction models.

The models are trained on a CSV file.  The CSV contains six features and two
targets, see ``common.py`` for the exact column layout.  Two separate models
are trained, one per target:

* ``demand-model.ubj`` predicts the annual heat demand in kWh
* ``peak-model.ubj`` predicts the peak load in kW

All raw values and codes are used directly, no factor mappings are applied.
Each model is saved as a single ``.ubj`` file in the server resources.  Use
``validate.py`` to validate the trained models.

Usage::

    uv run train.py data/training-data.csv
"""

import argparse
from pathlib import Path

import numpy as np
import xgboost as xgb

from common import DEMAND_MODEL_FILE, PEAK_MODEL_FILE, read_csv_data

# XGBoost training parameters, shared by both models.  There is no
# ``multi_strategy`` because each model predicts a single target.
PARAMS = {
    "objective": "reg:squarederror",
    "tree_method": "hist",
    "reg_alpha": 0.1,
    "eta": 0.5,  # learning rate
    "max_depth": 6,
}
NUM_ROUNDS = 1000


def train_model(
    features: np.ndarray, labels: np.ndarray, output_model_file: Path
) -> xgb.Booster:
    """Train one single-target model and save it to ``output_model_file``."""
    print(f"Train model: {output_model_file.name}")
    dtrain = xgb.DMatrix(features, label=labels)
    model = xgb.train(PARAMS, dtrain, NUM_ROUNDS)

    print(f"Saving model to: {output_model_file.name}")
    output_model_file.parent.mkdir(exist_ok=True, parents=True)
    # Save in binary JSON (UBJ) format for better cross-platform compatibility
    model.save_model(output_model_file)
    return model


def main():
    parser = argparse.ArgumentParser(
        description="Train the heat demand and peak load prediction models."
    )
    parser.add_argument(
        "training_data", type=Path, help="the training CSV file"
    )
    args = parser.parse_args()

    print(f"Read training data from: {args.training_data.name}")
    features, labels = read_csv_data(args.training_data)

    train_model(features, labels[:, 0], DEMAND_MODEL_FILE)
    train_model(features, labels[:, 1], PEAK_MODEL_FILE)

    print("All done!")


if __name__ == "__main__":
    main()
