"""Shared constants and helpers for the training and validation scripts.

The CSV column layout, the model locations and the ``read_csv_data`` function
live here so that ``train.py`` and ``validate.py`` always read the data and the
models in exactly the same way.
"""

import csv
from pathlib import Path

import numpy as np

# Column indices of the data CSV
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

# Locations of the two trained models expected by the server application.
MODEL_DIR = (
    Path(__file__).parent
    / "../src/main/resources/com/greendelta/bioheating/predict"
)
DEMAND_MODEL_FILE = MODEL_DIR / "demand-model.ubj"
PEAK_MODEL_FILE = MODEL_DIR / "peak-model.ubj"


def read_csv_data(csv_file: Path) -> tuple[np.ndarray, np.ndarray]:
    """Read the features and the two target columns from a data CSV.

    Returns the feature matrix (n x 6) and the label matrix (n x 2) as float32
    arrays.  The first row is always skipped and all values are read by column
    index, so the header names can be changed freely.
    """
    features = []
    labels = []

    with open(csv_file, "r", encoding="utf-8", newline="") as f:
        reader = csv.reader(f)
        next(reader, None)  # skip the header row

        for line, row in enumerate(reader, start=2):
            if not row:
                continue
            if len(row) < COL_COUNT:
                raise ValueError(
                    f"{csv_file.name}:{line} has {len(row)} columns, "
                    f"expected {COL_COUNT}"
                )

            features.append(_feature_of(row))
            labels.append(
                [float(row[COL_HEAT_DEMAND]), float(row[COL_PEAK_LOAD])]
            )

    if not features:
        raise ValueError(f"{csv_file.name} contains no data rows")

    return (
        np.array(features, dtype=np.float32),
        np.array(labels, dtype=np.float32),
    )


def _feature_of(row: list[str]) -> list[float]:
    return [
        float(row[COL_GROUND_AREA]),
        float(row[COL_HEIGHT]),
        float(row[COL_WEATHER_STATION]),
        float(row[COL_CONSTRUCTION_YEAR]),
        float(row[COL_ROOF_TYPE]),
        float(row[COL_BUILDING_TYPE]),
    ]
