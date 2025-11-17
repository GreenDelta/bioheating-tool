# Model training

This directory contains the script for training the XGBoost model used in the application. In order to run it, you first need to setup an environment, e.g. with `uv`:

```bash
cd server/model-training
uv venv --python 3.13

# training it on the CPU is fast enough for now
uv pip install xgboost-cpu numpy

# activate the environment
source .venv/bin/activate

# or on Windows PowerShell
.venv\Scripts\activate

# then, run the script
uv run train.py
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
../src/main/resources/com/greendelta/bioheating/predict/model.bin
```

This is the location expected by the server application, which loads the model at runtime. The script also runs a validation on `data/validation-data.csv` and writes a tab-separated file `data/validation-check.txt` of the following format:

```
expected	predicted
5657.60	5789.32
7833.60	7901.45
...
```
