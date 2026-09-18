"""Validate the trained model and write the resources for the model check.

The script loads the trained model, predicts both targets for the rows of the
given CSV file and writes a tab-separated check file with four columns:

    heat_expected  heat_predicted  peak_expected  peak_predicted

For ``data/validation-data.csv`` the check file is ``data/validation-check.txt``
(any other name gets a ``-check.txt`` suffix instead of ``-data.csv``).  The
check file can be plotted with GnuPlot, see ``model-check-plot.plt``.

The script also prints common regression statistics for both targets; the
meaning of these statistics is documented in the README.

Usage::

    uv run validate.py data/validation-data.csv
"""

import argparse
from pathlib import Path

import numpy as np
import xgboost as xgb

from common import MODEL_FILE, TARGET_NAMES, check_file_for, read_csv_data


def predict(model: xgb.Booster, csv_file: Path) -> tuple[np.ndarray, np.ndarray]:
    """Return the expected labels and the model predictions for ``csv_file``."""
    features, labels = read_csv_data(csv_file)
    predictions = model.predict(xgb.DMatrix(features))
    return labels, predictions


def write_check_file(
    out_file: Path, labels: np.ndarray, predictions: np.ndarray
) -> None:
    """Write the expected and predicted values of both targets to ``out_file``."""
    out_file.parent.mkdir(exist_ok=True, parents=True)
    with open(out_file, "w", encoding="utf-8") as f:
        for expected, predicted in zip(labels, predictions):
            f.write(
                f"{expected[0]}\t{predicted[0]}\t"
                f"{expected[1]}\t{predicted[1]}\n"
            )


def print_statistics(
    source: Path, labels: np.ndarray, predictions: np.ndarray
) -> None:
    """Print common regression statistics for both targets."""
    print(f"\nStatistics for {source} (n = {len(labels)})")

    for i, name in enumerate(TARGET_NAMES):
        expected = labels[:, i]
        predicted = predictions[:, i]
        error = expected - predicted

        mae = float(np.mean(np.abs(error)))
        rmse = float(np.sqrt(np.mean(error**2)))
        mbe = float(np.mean(error))
        ss_res = float(np.sum(error**2))
        ss_tot = float(np.sum((expected - np.mean(expected)) ** 2))
        r2 = 1 - ss_res / ss_tot if ss_tot > 0 else float("nan")

        print(f"\n  {name}")
        print(f"    MAE  = {mae:.3f}")
        print(f"    RMSE = {rmse:.3f}")
        print(f"    MBE  = {mbe:.3f}")
        print(f"    R2   = {r2:.4f}")


def main():
    parser = argparse.ArgumentParser(
        description="Validate the heat demand and peak load prediction model."
    )
    parser.add_argument("data", type=Path, help="the CSV file to validate")
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="the check file to write (default: <name>-check.txt next to the input)",
    )
    args = parser.parse_args()

    if not MODEL_FILE.is_file():
        raise SystemExit(f"Model file not found: {MODEL_FILE}. Run train.py first.")

    model = xgb.Booster()
    model.load_model(str(MODEL_FILE))

    labels, predictions = predict(model, args.data)

    out_file = args.output or check_file_for(args.data)
    write_check_file(out_file, labels, predictions)
    print(f"Wrote check file to: {out_file}")

    print_statistics(args.data, labels, predictions)


if __name__ == "__main__":
    main()
