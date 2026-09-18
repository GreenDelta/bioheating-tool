"""Shared constants and helpers for the training and validation scripts.

The CSV column layout, the model location and the ``read_csv_data`` function
live here so that ``train.py`` and ``validate.py`` always read the data and the
model in exactly the same way.  The CSV format itself is documented in
``xls_simout_to_csv.py``.
"""

import csv
from pathlib import Path

import numpy as np

from xls_simout_to_csv import CSV_HEADER

# Column indices of the data CSV, see xls_simout_to_csv.py.
COL_GROUND_AREA = 0
COL_HEIGHT = 1
COL_WEATHER_STATION = 2
COL_CONSTRUCTION_YEAR = 3
COL_ROOF_TYPE = 4
COL_BUILDING_TYPE = 5
COL_HEAT_DEMAND = 6
COL_PEAK_LOAD = 7
COL_COUNT = 8

# Number of model inputs and the names of the two targets (in output order).
FEATURE_COUNT = 6
TARGET_NAMES = ["heat demand", "peak load"]

# The location where the trained model is expected by the server application.
MODEL_FILE = (
    Path(__file__).parent
    / "../src/main/resources/com/greendelta/bioheating/predict/model.ubj"
)


def read_csv_data(csv_file: Path) -> tuple[np.ndarray, np.ndarray]:
    """Read the features and the two target columns from a data CSV.

    Returns the feature matrix (n x 6) and the label matrix (n x 2) as float32
    arrays.  The header is checked so that files in the old format are rejected
    instead of being silently misread.
    """
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


def check_file_for(csv_file: Path) -> Path:
    """Return the check file path that belongs to a data CSV.

    ``data/validation-data.csv`` becomes ``data/validation-check.txt`` and
    ``data/training-data.csv`` becomes ``data/training-check.txt``.
    """
    stem = csv_file.stem
    if stem.endswith("-data"):
        stem = stem[: -len("-data")]
    return csv_file.with_name(stem + "-check.txt")
