
# The Excel Import Format

The BioHeating tool supports an format for importing and updating building data
from an Excel file. The same format used when exporting building data, so an
exported file can be edited and re-imported.


## General rules

- The buildings provided by the Excel-Sheet are expected to be all heated and to be all included in the solution.
- Only the first sheet of the workbook is read; the sheet name does not matter.
- The first row contains the column headers and is ignored.
- The column order matters; fields are mapped by their column position.
- Blank cells are allowed for optional values.
- Text values are trimmed; comparisons ignore case.
- Booleans accept `x`, `y`, `yes`, `true`, `ja`, `j`, `1`; everything else is `false`.
- A row is skipped when it has no `name` or no valid `longitude`/`latitude`.
- Buildings are matched by their `id`. When there is no ID, they are also matched by their geometric intersection (see below)


## Columns

The table below shows which columns are mapped to which building attributes:

| # | Attribute           | Unit      | Required | Default value         |
|---|---------------------|-----------|----------|-----------------------|
| A | ID                  | –         | no       | generated             |
| B | Name                | –         | no       | street + number or id |
| C | Longitude           | °         | yes      | –                     |
| D | Latitude            | °         | yes      | –                     |
| E | Building type       | code      | no       | estimated             |
| F | Construction year   | code/year | no       | `4` (1979-1995)       |
| G | Height              | m         | no       | estimated             |
| H | Ground area         | m²        | no       | estimated             |
| I | Flat roof           | bool      | no       | `false`               |
| J | Warm water fraction | %         | no       | estimated             |
| K | Heat demand         | kWh/a     | no       | estimated             |
| L | Peak load           | kW        | no       | estimated             |
| M | City                | –         | no       | –                     |
| N | Postal code         | –         | no       | –                     |
| O | Street              | –         | no       | –                     |
| P | Number              | –         | no       | –                     |

### ID

— the building id from the source data (e.g. the CityGML id) or any
  other stable identifier.
  - Used to **find and update** existing buildings. Without it, a new building is
    created on every import.
  - Must be unique within the file.

- **`name`** — display name of the building.
  - When omitted for a new building, it is derived from `street` and `number`.
  - A row without a name (and without a derivable address) is skipped.
- **`longitude`** / **`latitude`** — geographic position in **WGS84** decimal
  degrees.
  - The building footprint is projected into the UTM zone of the project; when a
    building is created, a small square around the point is used as footprint.
  - Rows without a valid coordinate pair (0, out of range) are skipped.
- **`city`** / **`postal code`** / **`street`** / **`number`** — the address of
  the building (`locality`, `postalCode`, `street`, `streetNumber` in the model).
- **`building type`** — the type of the building; a **code (1-10)** or a
  label is accepted (see [Codes](#codes)).
  - Fallback: if the value is missing or unknown, the type is **estimated from
    `ground area` and `height`** (e.g. small footprint + low height → single
    family, large footprint → multi-family, tall → high-rise). If that is not
    possible, it defaults to `10` (multi-generation).
- **`construction year`** — the construction age of the building. Accepts:
  - a **code** `< 8` (see [Codes](#codes)), e.g. `4`
  - a **year** as an integer, e.g. `1994` (mapped to the matching age range)
  - a **range string**, e.g. `1979-1994` or `1949-1978` (mapped to the matching
    age range)
  - Fallback: `4` / `1979-1995` when missing or unknown.
- **`roof type`** — `flat` / `0` or `pitched` / `1`.
  - Fallback: pitched.
- **`ground area`** — the ground surface area of the building in m².
  - This is a **model input** for the demand estimation.
- **`height`** — the building height in m.
  - This is a **model input** for the demand estimation.
- **`heat demand`** — annual heat demand in kWh/a.
  - When given, the value is used **as is** (manual override).
  - When empty, the value is **estimated** from the model inputs.
- **`peak load`** — peak heating load in kW.
  - When given, the value is used **as is** (manual override).
  - When empty, the value is **estimated** from the model inputs.
- **`warm water fraction`** — share of the heat demand that is warm water, in %.
  - Fallback: the value for the building type and construction year, otherwise
    `14` %.
- **`is heated`** — marks the building as a heat consumer.
  - Fallback: `true` when `heat demand` **and** `peak load` are greater than `0`.
- **`is included`** — marks the building as part of the selected network scope.
  - Fallback: `true`.

### Not a column (for now)

- **Climate region / weather station** — this is a **project** property, not a
  building property. It is determined once from the project location (the
  coordinates of the whole dataset), so it does not need a per-building column.

## How heat demand and peak load are estimated

When `heat demand` and `peak load` are not given, they are estimated with the two
trained models. The models use these six inputs:

- `ground area` [m²]
- `height` [m]
- `construction year` (age code)
- `roof type` (flat / pitched)
- `building type` (code)
- climate region / weather station code (from the project location)

Everything except the weather station comes from the columns above. Missing
inputs fall back to the defaults listed in the column table, so a row with only a
name, a coordinate and a ground area still produces a result.

## Codes

### Building type (`building type`)

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
| 10   | multi-generation house (default) |

### Construction year (`construction year`)

| Code | Range       | Accepted forms              |
|------|-------------|-----------------------------|
| 1    | 1900-1919   | `1`, `1910`, `1900-1919`    |
| 2    | 1919-1948   | `2`, `1930`, `1919-1948`    |
| 3    | 1949-1978   | `3`, `1960`, `1949-1978`    |
| 4    | 1979-1995   | `4`, `1990`, `1979-1995` (default) |
| 5    | 1995-2009   | `5`, `2000`, `1995-2009`    |
| 6    | 2010-2030   | `6`, `2015`, `2010-2030`    |

### Roof type (`roof type`)

| Value           | Meaning                    |
|-----------------|----------------------------|
| `0`, `flat`     | flat roof (Flachdach)      |
| `1`, `pitched`  | pitched roof (Satteldach, default) |






The format is designed to be *permissive*: only a few fields are really needed,
everything else has a sensible fallback. The main purpose of the additional
columns is to provide the inputs that are needed to **estimate the heat demand
and the peak load** of a building.

> This is a first draft. The field list, the fallback rules and the code tables
> below are proposals that we will refine.
