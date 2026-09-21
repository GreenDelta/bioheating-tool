# Model training

This directory contains the Python scripts for converting the simulation output,
for training and for validating the XGBoost models used in the application. The
application uses two separate models: one for the annual heat demand in kWh and
one for the peak load in kW.

## Setup & usage

The project uses [uv](https://docs.astral.sh/uv/) for Python dependency
management. The required dependencies and Python version (3.13) are configured
in `pyproject.toml`.

```bash
cd server/model-training

# install dependencies (creates .venv automatically)
uv sync

# convert the simulation output Excel files to the CSV format
uv run xls_simout_to_csv.py training-simout.xlsx data/training-data.csv

# train the models (creates demand-model.ubj and peak-model.ubj)
uv run train.py data/training-data.csv

# validate the model (writes data/validation-check.* and prints statistics)
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

The header names are only documentation: the scripts always read the values by
column index and ignore the header row, so the header names can be changed
freely.

### Codes

All codes are used directly as model features, there are no factor mappings.

Construction year (column 3):

| Range     | Code        |
|-----------|-------------|
| 1900-1919 | 1           |
| 1919-1948 | 2           |
| 1949-1978 | 3           |
| 1979-1995 | 4 (default) |
| 1995-2009 | 5           |
| 2010-2030 | 6           |

Roof type (column 4): `1` = flat roof (Flachdach), `0` = pitched roof
(Satteldach).

Building type (column 5):

| Code | Meaning                     |
|------|-----------------------------|
| 1    | high house                  |
| 2    | small multi-family house    |
| 3    | medium multi-family house   |
| 4    | large multi-family house    |
| 5    | Gebäudeteil (building part) |
| 6    | one-family house            |
| 7    | end row house               |
| 8    | middle row house            |
| 9    | group of houses             |
| 10   | multi-generation house      |

## Training

`train.py` reads the given training CSV and trains two separate models, one per
target:

```bash
uv run train.py data/training-data.csv
```

The trained models are saved to:

```
../src/main/resources/com/greendelta/bioheating/predict/demand-model.ubj
../src/main/resources/com/greendelta/bioheating/predict/peak-model.ubj
```

These are the locations expected by the server application, which loads both
models at runtime: `demand-model.ubj` predicts the annual heat demand in kWh and
`peak-model.ubj` predicts the peak load in kW.

## Validation

`validate.py` loads both trained models, predicts both targets for the given
CSV, writes the check files and prints statistics:

```bash
uv run validate.py data/validation-data.csv
```

Both check files are always written next to the input file, so both the
self-validation on the training data
(`uv run validate.py data/training-data.csv`) and the validation on the
validation data produce the same files:

+ `validation-check.txt` - tab-separated, with the columns `heat_expected`,
  `heat_predicted`, `peak_expected` and `peak_predicted`, used by GnuPlot.
+ `validation-check.xlsx` - Excel file with the columns `heat demand expected
  [kWh]`, `heat demand predicted [kWh]`, `peak load expected [kW]` and
  `peak load predicted [kW]`.

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

