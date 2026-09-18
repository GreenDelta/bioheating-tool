# Model training

This directory contains the Python scripts for converting the simulation output,
for training and for validating the XGBoost model used in the application. The
model is a single booster with two outputs: the annual heat demand in kWh and the
peak load in kW.

## Setup & usage

The project uses [uv](https://docs.astral.sh/uv/) for Python dependency management. The required
dependencies and Python version (3.13) are configured in `pyproject.toml`.

```bash
cd server/model-training

# install dependencies (creates .venv automatically)
uv sync

# convert the simulation output Excel files to the CSV format
uv run xls_simout_to_csv.py training-simout.xlsx data/training-data.csv

# train the model (creates the model.ubj resource)
uv run train.py data/training-data.csv

# validate the model (writes data/validation-check.txt and prints statistics)
uv run validate.py data/validation-data.csv

# optional: self-check on the training data (writes the same check file)
uv run validate.py data/training-data.csv

# create the charts from the check file
gnuplot model-check-plot.plt

# clean up generated check files and charts
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

## Training

`train.py` reads the given training CSV and trains one model for both targets:

```bash
uv run train.py data/training-data.csv
```

The trained model is saved to:

```
../src/main/resources/com/greendelta/bioheating/predict/model.ubj
```

This is the location expected by the server application, which loads the model
at runtime. The model has two outputs: index 0 is the heat demand in kWh and
index 1 is the peak load in kW.

## Validation

`validate.py` loads the trained model, predicts both targets for the given CSV
and writes the check file and prints statistics:

```bash
uv run validate.py data/validation-data.csv
```

The check file is always written as `data/validation-check.txt` next to the
input file, so both the self-validation on the training data
(`uv run validate.py data/training-data.csv`) and the validation on the
validation data produce the same file. It contains four tab-separated columns:

```
heat_expected  heat_predicted  peak_expected  peak_predicted
```

### Statistics

For both targets the script prints the following statistics:

| Statistic | Meaning                                                                                                                                                                               |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| n         | Number of rows that were validated.                                                                                                                                                   |
| MAE       | Mean absolute error: the average absolute deviation between expected and predicted values, in the unit of the target (kWh or kW). Lower is better.                                     |
| RMSE      | Root mean squared error: like MAE, but large errors are weighted more. Same unit as the target. Lower is better.                                                                       |
| MBE       | Mean bias error (mean of expected − predicted): the average systematic deviation. Positive means the model under-predicts on average, negative means it over-predicts. Ideal is 0.      |
| R2        | Coefficient of determination: the fraction of the variance of the expected values that the model explains. 1.0 is a perfect fit, 0.0 is no better than always predicting the mean, negative values are worse than the mean. |

### Charts

The check file can be plotted with GnuPlot:

```bash
gnuplot model-check-plot.plt
```

This creates `data/model-check-heat-demand.png` and
`data/model-check-peak-load.png` from `data/validation-check.txt`. Since both
the self-validation and the validation write to that same file, run the
`validate.py` command for the data you want to see before running gnuplot.

> **Note:** the simulation Excel and CSV format changed with the new model, so
> the Java example classes (`ModelTrainingExample`, `ModelValidationExample`)
> still use the old format and need to be updated to match this document.

