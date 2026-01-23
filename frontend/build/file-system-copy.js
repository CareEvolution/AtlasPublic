const fs = require('fs');
const path = require('path');

const copyFile = (src, dest) => {
	if (!fs.existsSync(src)) {
		console.log(`Source file ${src} does not exist. Skipping... `);
		return;
	}
	if (!fs.existsSync(path.dirname(dest))) {
		fs.mkdirSync(path.dirname(dest), { recursive: true });
	}
	fs.copyFileSync(src, dest);
};

const copyDirectory = (src, dest) => {
	if (!fs.existsSync(src)) {
		console.log(`Source directory ${src} does not exist. Skipping... `);
		return;
	}
	if (!fs.existsSync(dest)) {
		fs.mkdirSync(dest, { recursive: true });
	}
	const entries = fs.readdirSync(src, { withFileTypes: true });
	for (const entry of entries) {
		const srcPath = path.join(src, entry.name);
		const destPath = path.join(dest, entry.name);
		if (entry.isDirectory()) {
			copyDirectory(srcPath, destPath);
		} else {
			copyFile(srcPath, destPath);
		}
	}
};

const copySrcToDest = (src, dest) => {
	if (fs.existsSync(src)) {
		if (fs.lstatSync(src).isDirectory()) {
			copyDirectory(src, dest);
		} else {
			copyFile(src, dest);
		}
	} else {
		console.log(`Source ${src} does not exist. Skipping... `);
	}
};

if (typeof define !== 'undefined') {
	define(() => copySrcToDest);
} else {
	module.exports = copySrcToDest;
}
