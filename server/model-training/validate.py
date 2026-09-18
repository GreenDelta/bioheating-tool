"""Validate the trained models and write the resources for the model check.

The script loads the two trained models, predicts both targets for the rows of
the given CSV file and writes two check files next to the input CSV:

* ``validation-check.txt`` - tab-separated, for plotting with GnuPlot
  (columns: ``heat_expected  heat_predicted  peak_expected  peak_predicted``)
* ``validation-check.xlsx`` - Excel file with the columns
  ``heat demand expected [kWh]``, ``heat demand predicted [kWh]``,
  ``peak load expected [kW]`` and ``peak load predicted [kW]``

The GnuPlot script is ``model-check-plot.plt``.

The script also prints common regression statistics for both targets; the
meaning of these statistics is documented in the README.

Usage::

    uv run validate.py data/validation-data.csv
"""

import argparse
from pathlib import Path

import numpy as np
import xgboost as xgb
from openpyxl import Workbook

from common import (
    DEMAND_MODEL_FILE,
    PEAK_MODEL_FILE,
    TARGET_NAMES,
    read_csv_data,
)


def load_models() -> tuple[xgb.Booster, xgb.Booster]:
    """Load the demand and the peak load model from the server resources."""
    return _load_model(DEMAND_MODEL_FILE), _load_model(PEAK_MODEL_FILE)


def _load_model(model_file: Path) -> xgb.Booster:
    if not model_file.is_file():
        raise SystemExit(
            f"Model file not found: {model_file}. Run train.py first."
        )
    model = xgb.Booster()
    model.load_model(str(model_file))
    return model


def predict(
    demand_model: xgb.Booster, peak_model: xgb.Booster, csv_file: Path
) -> tuple[np.ndarray, np.ndarray]:
    """Return the expected labels and the predictions of both models."""
    features, labels = read_csv_data(csv_file)
    matrix = xgb.DMatrix(features)
    predictions = np.column_stack(
        [demand_model.predict(matrix), peak_model.predict(matrix)]
    )
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


EXCEL_HEADER = [
    "heat demand expected [kWh]",
    "heat demand predicted [kWh]",
    "peak load expected [kW]",
    "peak load predicted [kW]",
]


def write_excel_file(
    out_file: Path, labels: np.ndarray, predictions: np.ndarray
) -> None:
    """Write expected and predicted values of both targets to an Excel file."""
    out_file.parent.mkdir(exist_ok=True, parents=True)
    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "validation"
    sheet.append(EXCEL_HEADER)
    for expected, predicted in zip(labels, predictions):
        sheet.append(
            [
                float(expected[0]),
                float(predicted[0]),
                float(expected[1]),
                float(predicted[1]),
            ]
        )
    workbook.save(out_file)


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


def check_file_for(csv_file: Path) -> Path:
    """Return the check file path for a data CSV.

    The check file is always named ``validation-check.txt`` and written next to
    the input CSV.
    """
    return csv_file.with_name("validation-check.txt")


def main():
    parser = argparse.ArgumentParser(
        description="Validate the heat demand and peak load prediction model."
    )
    parser.add_argument("data", type=Path, help="the CSV file to validate")
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help=(
            "the check file to write (default: validation-check.txt next to "
            "the input; the Excel file uses the same name with .xlsx)"
        ),
    )
    args = parser.parse_args()

    demand_model, peak_model = load_models()
    labels, predictions = predict(demand_model, peak_model, args.data)

    out_file = args.output or check_file_for(args.data)
    write_check_file(out_file, labels, predictions)
    print(f"Wrote check file to: {out_file}")

    excel_file = out_file.with_suffix(".xlsx")
    write_excel_file(excel_file, labels, predictions)
    print(f"Wrote Excel file to: {excel_file}")

    print_statistics(args.data, labels, predictions)


if __name__ == "__main__":
    main()
