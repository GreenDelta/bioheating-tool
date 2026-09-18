"""Train the multi-output model for heat demand and peak load prediction.

The model is trained on a CSV file.  The CSV contains six features and two
targets, see ``common.py`` for the exact column layout.

All raw values and codes are used directly, no factor mappings are applied.
Both targets are predicted by a single model, i.e. the model has two outputs.
XGBoost handles this as a multi-output regression: the label matrix has two
columns and ``multi_strategy`` controls how the trees are built (see PARAMS).
The trained booster is saved as a single ``model.ubj`` file in the server
resources.  Use ``validate.py`` to validate the trained model.

Usage::

    uv run train.py data/training-data.csv
"""

import argparse
from pathlib import Path

import xgboost as xgb

from common import MODEL_FILE, read_csv_data

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


def train_model(training_file: Path, output_model_file: Path) -> xgb.Booster:
    """Train the multi-output model and save it to ``output_model_file``."""
    print(f"Read training data from: {training_file.name}")
    features, labels = read_csv_data(training_file)
    dtrain = xgb.DMatrix(features, label=labels)

    print("Train model with ...")
    model = xgb.train(PARAMS, dtrain, NUM_ROUNDS)

    print(f"Saving model to: {output_model_file.name}")
    output_model_file.parent.mkdir(exist_ok=True, parents=True)
    # Save in binary JSON (UBJ) format for better cross-platform compatibility
    model.save_model(output_model_file)
    return model


def main():
    parser = argparse.ArgumentParser(
        description="Train the heat demand and peak load prediction model."
    )
    parser.add_argument(
        "training_data", type=Path, help="the training CSV file"
    )
    args = parser.parse_args()

    train_model(args.training_data, MODEL_FILE)
    print("All done!")


if __name__ == "__main__":
    main()
