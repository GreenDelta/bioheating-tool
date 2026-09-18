# Model training

This directory contains the Python scripts for converting the simulation output
and for training the XGBoost model used in the application. The model is a
single booster with two outputs: the annual heat demand in kWh and the peak load
in kW.

## Setup & usage

The project uses [uv](https://docs.astral.sh/uv/) for Python dependency management. The required
dependencies and Python version (3.13) are configured in `pyproject.toml`.

```bash
cd server/model-training

# install dependencies (creates .venv automatically)
uv sync

# convert the simulation output Excel files to the training CSV format
uv run xls_simout_to_csv.py training-simout.xlsx data/training-data.csv

# run the training script
uv run train.py

# clean up generated check files
uv run clean.py

# clean up including model files
uv run clean.py --all
```

## Data format

The training data is converted from the simulation output Excel file with
`xls_simout_to_csv.py`. All details about the Excel columns and the conversion
decisions are documented in that script.

The conversion produces the following CSV files in the
`server/model-training/data` folder:

+ `training-data.csv`
+ `validation-data.csv`

The first six columns are the model features, columns 6 and 7 are the two
prediction targets:

| Column | Header                   | Type    | Description                         |
|--------|--------------------------|---------|-------------------------------------|
| 0      | ground area [m2]         | Float   | Ground surface area of the building |
| 1      | height [m]               | Float   | Building height                     |
| 2      | weather station [code]   | Integer | Weather station code (1-15)         |
| 3      | construction year [code] | Integer | Construction age code (1-6)         |
| 4      | roof type [1\|0]         | Integer | 1 = flat roof, 0 = pitched roof      |
| 5      | building type [code]     | Integer | Building type code (1-10)           |
| 6      | heat demand [kWh]        | Float   | Target: annual heat demand          |
| 7      | peak load [kW]           | Float   | Target: maximum heat load           |

All raw values and codes are used directly as model features, there are no
factor mappings.

## Generated output

The trained model is saved to:

```
../src/main/resources/com/greendelta/bioheating/predict/model.ubj
```

This is the location expected by the server application, which loads the model
at runtime. The model has two outputs: index 0 is the heat demand in kWh and
index 1 is the peak load in kW.

The script also runs a validation on `data/validation-data.csv` and a self check
with its own training data. It writes the tab-separated files
`data/validation-check.txt` and `data/self-check.txt`, each with four columns:

```
heat_expected  heat_predicted  peak_expected  peak_predicted
```

These files can be plotted using GnuPlot, see the `model-check-plot.plt` script.
It produces the two images `data/model-check-heat-demand.png` and
`data/model-check-peak-load.png`.

> **Note:** the simulation Excel and CSV format changed with the new model, so
> the Java example classes (`ModelTrainingExample`, `ModelValidationExample`)
> still use the old format and need to be updated to match this document.

