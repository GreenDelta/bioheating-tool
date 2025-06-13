# The BioHeating Tool

## Building the UI

The user interface is built with TypeScript, React, and Webpack. The UI source code is located in the `ui/` directory.

### Development

For development with automatic rebuilding on file changes:

```bash
cd ui
npm run dev
```

This runs `webpack watch` mode, which automatically rebuilds the UI when source files change and outputs the built files to `server/static/`.

### Production Build

For a production build:

```bash
cd ui
npm run build
```

This runs webpack in production mode and outputs the built files to `server/static/`.

### Build Output

The webpack build process:
- Compiles TypeScript/React source files from `ui/src/` into `main.js`
- Copies dependencies and assets to `server/static/`:
  - `bootstrap.min.css` and `bootstrap.bundle.min.js` from Bootstrap
  - `leaflet.css` from Leaflet
  - `index.html` from `ui/src/`
  - `home.png` from `ui/img/`
  - Generated `main.js` and `main.js.map` files

The built files in `server/static/` are served by the Spring Boot application.

## Todo

+ allow multiple CityGML files per project
+ import streets from OSM when importing CityGML
+ configure an upload folder (for CityGML files)
