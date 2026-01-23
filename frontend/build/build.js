// This script builds and minifies the JavaScript and CSS files for the project.
// It uses RequireJS for module loading, Babel for transpiling, and Terser for minification.
// It also copies necessary assets to the distribution folder.
// Finally, it updates the `index.html` file to reference the minified JavaScript file with a hash for cache busting.
// In a modern web app all of this would be done by a build tool like Webpack or Rollup, but this is a legacy project
// that uses RequireJS. All of the build steps are done in this script.

const babel = require('@babel/core');
const r = require('requirejs');
const projectSettings = require('../js/settings');
const copySrcToDest = require('./file-system-copy');
const terser = require('terser');
const fs = require('fs');
const crypto = require('crypto');

const minifyAndWriteJavascriptToFile = (optimizeOutputFile) => {

	const text = fs.readFileSync(optimizeOutputFile, 'utf8');
	fs.unlinkSync(optimizeOutputFile);

	console.log('Minifying with Terser...');
	return terser.minify(text, { sourceMap: true }).then((result) => {
		if (result.error) {
			console.error('Terser error:', result.error);
			throw result.error;
		}
		const destination = 'dist/js';
		if (!fs.existsSync(destination)) {
			fs.mkdirSync(destination, { recursive: true });
		}

		// Compute a hash for the terser output
		console.log('Hashing minified content...');
		const hash = crypto.createHash('md5').update(result.code).digest('hex').substring(0, 8);
		const outputFileName = `main-${hash}`;

		console.log(`Writing minified file...`);
		fs.writeFileSync(destination + `/${outputFileName}.js`, result.code);
		fs.writeFileSync(destination + `/${outputFileName}.js.map`, result.map);

		// Update index.html to use the new filename
		console.log('Updating index.html to reference new script...');
		const indexPath = 'dist/index.html';
		if (fs.existsSync(indexPath)) {
			let indexContent = fs.readFileSync(indexPath, 'utf8');
			indexContent = indexContent.replace(
				/<script data-main="js\/main" src="require\.js"><\/script>/,
				`<script data-main="js/${outputFileName}" src="require.js"></script>`
			);
			fs.writeFileSync(indexPath, indexContent);
		} else {
			console.warn('index.html not found. Skipping update to script tag.');
		}
		console.log(`JavaScript processing complete.`);
	});
};

const writeCssToFile = (optimizeOutputFile) => {

	const text = fs.readFileSync(optimizeOutputFile, 'utf8');
	fs.unlinkSync(optimizeOutputFile);

	const destination = 'dist/styles';
	if (!fs.existsSync(destination)) {
		fs.mkdirSync(destination, { recursive: true });
	}

	// Compute a hash for the terser output
	console.log('Hashing css...');
	const hash = crypto
		.createHash('md5')
		.update(text)
		.digest('hex')
		.substring(0, 8);
	const outputFileName = `main-${hash}`;

	console.log(`Writing css file...`);
	fs.writeFileSync(destination + `/${outputFileName}.css`, text);

	// Update index.html to use the new filename
	console.log('Updating index.html to reference new css...');
	const indexPath = 'dist/index.html';
	if (fs.existsSync(indexPath)) {
		let indexContent = fs.readFileSync(indexPath, 'utf8');
		indexContent = indexContent.replace(
			/<link rel="stylesheet" href="styles\/main.css" \/>/,
			`<link rel="stylesheet" href="styles/${outputFileName}.css" />`
		);
		fs.writeFileSync(indexPath, indexContent);
	} else {
		console.warn('index.html not found. Skipping update to link tag.');
	}
	console.log(`Css processing complete.`);
};

// shim for less plugin
global.window = global;
window.less = {
	sheets: [],
	refresh: () => {},
	options: {
		logLevel: 0,
	},
};

const jsSettings = {
	...projectSettings,
	paths: {
		...projectSettings.paths,
		...projectSettings.localRefs,
	},
	include: [],
	normalizeDirDefines: 'all',
	name: 'main',
	out: 'dist/js/main.js',
	onBuildRead(moduleName, path, content) {
		return babel.transform(content, {
			plugins: [],
			presets: [
				[
					'@babel/preset-env',
					{
					},
				],
			],
		}).code;
	},
	findNestedDependencies: true,
	generateSourceMaps: false,
	optimize: 'none',
};

const cssSettings = {
	optimizeCss: 'standard',
	cssIn: 'styles/main.css',
	out: 'dist/styles/main.css',
};

const assetsSettings = [
	{
		appDir: 'node_modules/font-awesome/fonts',
		dir: 'dist/styles/vendor/font-awesome/fonts',
	},
	{
		appDir: 'node_modules/jquery-ui/dist/themes/smoothness/images',
		dir: 'dist/styles/vendor/jquery-ui/dist/themes/smoothness/images',
	},
	{
		appDir: 'styles/images',
		dir: 'dist/styles/images',
	},
	{
		appDir: 'images',
		dir: 'dist/images',
	},
	{
		appDir: 'index.html',
		dir: 'dist/index.html',
	},
	{
		appDir: 'node_modules/requirejs/require.js',
		dir: 'dist/require.js',
	}
];

console.log('Copying assets to distribution folder...');
assetsSettings.forEach((settings) =>
	copySrcToDest(settings.appDir, settings.dir)
);

console.log('Optimizing JavaScript...');
r.optimize(
	jsSettings,
	() => {
		minifyAndWriteJavascriptToFile(jsSettings.out)
			.then(() => {
				console.log('Optimizing CSS...');
				r.optimize(cssSettings, () => {
					writeCssToFile(cssSettings.out);
				}, (err) => {
					console.error('Error during CSS optimization:', err);
				});
			})
			.catch((err) => {
				console.error('Error during JavaScript build:', err);
			});
	},
	function (err) {
		console.error('Error during JavaScript optimization:', err);
	}
);
