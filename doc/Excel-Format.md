
# The Excel Import Format

The BioHeating tool supports an format for importing and updating building data
from an Excel file. The same format used when exporting building data, so an
exported file can be edited and re-imported to efficiently update building data. An Excel file can be directly provided with the initial CityGML file(s) when creating a project. Alternatively, a project can be updated also later with such an Excel file. Provided values in an Excel file always overwrite the respective building attributes.


## General rules

- The buildings provided by the Excel-Sheet are expected to be all heated and to be all included in the solution.
- Only the first sheet of the workbook is read; the sheet name does not matter.
- The first row contains the column headers and is ignored.
- The column order matters; fields are mapped by their column position.
- Blank cells are allowed for optional values.
- Text values are trimmed; comparisons ignore case.
- Booleans accept `x`, `y`, `yes`, `true`, `ja`, `j`, `1`; everything else is `false`.
- A row is skipped when it has no valid `longitude`/`latitude`, or when no name
  can be derived from the name, the street and number, or the ID.
- Buildings are matched by their `id`. When there is no ID, they are also matched by their geometric intersection (see below)


## Columns

The table below shows which columns are mapped to which building attributes.
The `Header` column is the header text that the export writes; the import maps
the fields by their column position, so only the order matters:

| # | Header                | Attribute           | Unit      | Required | Default value         |
|---|-----------------------|---------------------|-----------|----------|-----------------------|
| A | `id`                  | ID                  | –         | no       | generated             |
| B | `name`                | Name                | –         | no       | street + number or id |
| C | `longitude`           | Longitude           | °         | yes      | –                     |
| D | `latitude`            | Latitude            | °         | yes      | –                     |
| E | `building type`       | Building type       | code      | no       | estimated             |
| F | `construction year`   | Construction year   | code/year | no       | `4` (1979-1995)       |
| G | `height`              | Height              | m         | no       | estimated             |
| H | `ground area`         | Ground area         | m²        | no       | estimated             |
| I | `flat roof`           | Flat roof           | bool      | no       | `false`               |
| J | `warm water fraction` | Warm water fraction | %         | no       | estimated             |
| K | `heat demand`         | Heat demand         | kWh/a     | no       | estimated             |
| L | `peak load`           | Peak load           | kW        | no       | estimated             |
| M | `city`                | City                | –         | no       | –                     |
| N | `postal code`         | Postal code         | –         | no       | –                     |
| O | `street`              | Street              | –         | no       | –                     |
| P | `number`              | Number              | –         | no       | –                     |


### ID

This is the building ID from the source data (e.g. the CityGML ID) or any other stable identifier. When updating a project it is used to find possible matching buildings (beside the geometry matching; see below). If not provided, and no matching building was found in an update, it will be simply generated. Note that it must be unique within a project.


### Name

This is the display name of the building. In the CityGML and when not provided, the import tries to derive it from the address information (street and number). If there is also no address information available, the ID is copied here.


### Longitude and latitude

This is the geographic position of the building in WGS84 decimal degrees, for example for a point in Berlin:

```
13.37779496958173, 52.51743035298201
```

(Note that the order is longitude (x) and latitude (y) in the Excel file but when you copy coordinates from Google Maps or OpenStreetmap they are typically given in latitude-longitude order that you need to flip then).

These coordinates are required. Rows without them are skipped.

This point is projected into the UTM zone of the project. From the ground-area of the building, a square around this point is created as the polygon of the polygon in the map. If no matching building was found by the building ID, the square is used to search for an existing building in the map. If it is intersecting with the polygon of an existing building this existing building is updated with the new attributes. The polygon of the existing building is replaced with the square, if the area of the square is larger than the area of the polygon.


### Building type

For the building type, the integer codes from the table below are accepted. If the code is missing or invalid the building type is estimated from provided attributes (ground area, height, number of neighbors).

| Code | Meaning                   |
|------|---------------------------|
| 1    | high house                |
| 2    | small multi-family house  |
| 3    | medium multi-family house |
| 4    | large multi-family house  |
| 5    | building part             |
| 6    | one-family house          |
| 7    | end row house             |
| 8    | middle row house          |
| 9    | group of houses           |
| 10   | multi-generation house    |


### Construction year

For the construction year, the import accepts values of the following form:

- a code `< 7`,
- a year as an integer, or
- a range string

If it cannot determine the construction age from the provided input or when no value is provided, it falls back to the default: `4` / `1979-1995`

| Code | Range     | Accepted forms                     |
|------|-----------|------------------------------------|
| 1    | 1900-1919 | `1`, `1910`, `1900-1919`           |
| 2    | 1919-1948 | `2`, `1930`, `1919-1948`           |
| 3    | 1949-1978 | `3`, `1960`, `1949-1978`           |
| 4    | 1979-1995 | `4`, `1990`, `1979-1995` (default) |
| 5    | 1995-2009 | `5`, `2000`, `1995-2009`           |
| 6    | 2010-2030 | `6`, `2015`, `2010-2030`           |


### Height and ground area

The height (m) and and ground area (m²) are provided as numbers. If not values are given, they are estimated from the building type.


### Roof type

The model currently only distinguishes between flat and pitched roofs. If the value set in this column evaluates to `true`, the roof type is set to `flat`, otherwise it is set to `pitched`. A blank cell means that no value is provided: an existing building keeps its roof type and a new building defaults to a pitched roof.


### Warm water fraction

This value is given in %. If not provided it is estimated from the building type and construction year.


### Heat demand and peak load

The annual heat demand  (kWh/a) and peak heating load (kW) can be provided as numbers. If these values are missing, the are estimated by the model.


### Address of the building

It is recommended to provide at least the street name and number here.
