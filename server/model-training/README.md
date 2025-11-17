# Model training

This directory contains the Python script for training the XGBoost model used in the application. The model can be also trained from Java, see the ModelTrainingExample.

## Setup & usage

The project uses [uv](https://docs.astral.sh/uv/) for Python dependency management. The required dependencies and Python version (3.13) are configured in `pyproject.toml`.

```bash
cd server/model-training

# install dependencies (creates .venv automatically)
uv sync

# run the training script
uv run train.py

# clean up generated check files
uv run clean.py

# Clean up including model files
uv run clean.py --all
```

## Data format

Before you can run the script, put the training and validation data into the `server/model-training/data` folder:

+ `trainig-data.csv`
+ `validation-data.csv`

The files need to have the following format:

| Column | Field               | Type    | Description                        |
|--------|---------------------|---------|------------------------------------|
| 0      | buildingId          | String  | Building identifier                |
| 1      | height              | Float   | Building height in meters          |
| 2      | storeys             | Integer | Number of storeys                  |
| 3      | groundArea          | Float   | Ground floor area in square meters |
| 4      | buildingTypeCode    | Integer | Building type code (0-9)           |
| 5      | climateRegionCode   | Integer | Climate region code (1-15)         |
| 6      | constructionAgeCode | Integer | Construction age code (0-6)        |
| 7      | roofTypeFactor      | Float   | Roof type factor (0.0-1.0)         |
| 8      | heatDemand          | Float   | Annual heat demand in kWh          |


## Generated output

The trained model is saved to:

```
../src/main/resources/com/greendelta/bioheating/predict/model.ubj
```

This is the location expected by the server application, which loads the model
at runtime. The script also runs a validation on `data/validation-data.csv` and
a self check with its own training data. It writes the tab-separated files
`data/validation-check.txt` and `data/self-check.txt`. These files can be then
nicely plotted using GnuPlot, see the `model-check-plot.plt` script.
