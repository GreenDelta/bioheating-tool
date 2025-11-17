# Model Training

This directory contains the script for training the XGBoost model used in the application.

### Setting up the environment with `uv`

Create a virtual environment and install dependencies:

```bash
cd server/model-training
uv venv --python 3.13
uv pip install xgboost-cpu numpy
```

Activate the virtual environment:

```bash
source .venv/bin/activate
```

or on Windows:

```powershell
.venv\Scripts\activate
```

Then, run the training script:

```bash
uv run train.py
```

## Input Data Format

The training and validation data are CSV files with the following columns:

| Column | Name | Type | Description |
|--------|------|------|-------------|
| 0 | buildingId | String | Unique building identifier |
| 1 | height | Float | Building height in meters |
| 2 | storeys | Integer | Number of storeys |
| 3 | groundArea | Float | Ground floor area in square meters |
| 4 | buildingTypeCode | Integer | Building type code (0-9) |
| 5 | climateRegionCode | Integer | Climate region code (1-15) |
| 6 | constructionAgeCode | Integer | Construction age code (0-6) |
| 7 | roofTypeFactor | Float | Roof type factor (0.0-1.0) |
| 8 | heatDemand | Float | Annual heat demand in kWh |

### Building Type Codes

- 0: Other
- 1: High-rise
- 2: Multi-family small
- 3: Multi-family medium
- 4: Multi-family large
- 5: Building part
- 6: Single-family
- 7: End terrace
- 8: Mid terrace
- 9: House group

### Construction Age Codes

- 0: Unknown
- 1: 1900-1919
- 2: 1919-1948
- 3: 1949-1978
- 4: 1979-1995
- 5: 1995-2009
- 6: 2010-2030

### Climate Region Codes

Climate regions 1-15 represent different climate zones with varying heat demand factors.

## Feature Engineering

The model uses 7 engineered features for training:

1. **height**: Building height in meters (from CSV)
2. **storeys**: Number of storeys (from CSV)
3. **groundArea**: Ground floor area in square meters (from CSV)
4. **buildingTypeFactor**: Factor based on building type (0.65-1.0)
5. **climateRegionFactor**: Factor based on climate region (0.8-1.19)
6. **averageHeatDemand**: Expected heat demand per m²/year based on construction age (50-210 kWh/m²/year)
7. **roofTypeFactor**: Roof type factor (from CSV)

## Model Configuration

The XGBoost model is trained with the following hyperparameters:

- **Objective**: `reg:squarederror` (regression task)
- **Tree method**: `hist` (histogram-based algorithm)
- **L1 regularization (reg_alpha)**: 0.1
- **Learning rate (eta)**: 0.5
- **Maximum tree depth**: 6
- **Number of boosting rounds**: 1000

These parameters match exactly with the Java implementation in `Training.java`.

## Output

### Trained Model

The trained model is saved to:
```
../src/main/resources/com/greendelta/bioheating/predict/model.bin
```

This is the location expected by the Java application, which loads the model at runtime.

### Validation Results

The script also runs validation on `data/validation-data.csv` and outputs:

1. **Console output**: Validation metrics (RMSE, MAE, MSE, R²)
2. **File output**: `data/validation-check.txt` - Tab-separated file with expected and predicted heat demand values

The validation file format:
```
expected	predicted
5657.60	5789.32
7833.60	7901.45
...
```

## File Structure

```
model-training/
├── train.py                    # Training script
├── README.md                   # This file
└── data/
    ├── training-data.csv       # Training dataset
    ├── validation-data.csv     # Validation dataset
    └── validation-check.txt    # Generated validation results
```

## Validation Metrics

The script calculates and displays the following metrics:

- **RMSE** (Root Mean Squared Error): Square root of the average squared differences between predicted and actual values. Lower is better.
- **MAE** (Mean Absolute Error): Average of absolute differences between predicted and actual values. Lower is better.
- **MSE** (Mean Squared Error): Average of squared differences between predicted and actual values. Lower is better.
- **R²** (Coefficient of Determination): Proportion of variance in the dependent variable that is predictable. Closer to 1.0 is better.

## Integration with Java Application

The trained model (`model.bin`) is automatically placed in the correct location for the Java application. The Java code loads this model using the XGBoost4J library:

```java
var model = XGBoost.loadModel(modelFile);
```

The feature encoding in Python exactly matches the encoding in `CsvEncoder.java` to ensure compatibility.
