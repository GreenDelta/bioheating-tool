const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = {
	entry: './src/main.tsx',
	mode: 'production',
	devtool: 'source-map',
	module: {
		rules: [
			{
				test: /\.tsx?$/,
				use: 'ts-loader',
				exclude: /node_modules/,
			},
		],
	},
	resolve: {
		extensions: ['.tsx', '.ts', '.js'],
	},
	output: {
		filename: 'main.js',
		path: path.resolve(__dirname, '../server/static'),
	},
	plugins: [
		new CopyWebpackPlugin({
			patterns: [
				{ from: 'node_modules/@picocss/pico/css/pico.min.css', to: './' },
				{ from: 'node_modules/leaflet/dist/leaflet.css', to: './' },
				{ from: 'src/index.html', to: './' },
			]
		})
	],
	performance: {
		hints: false
	}
};
