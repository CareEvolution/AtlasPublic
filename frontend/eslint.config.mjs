import { defineConfig } from 'eslint/config';
import js from '@eslint/js';
import requirejsPlugin from 'eslint-plugin-requirejs';
import globals from 'globals';

export default defineConfig([
	{
		ignores: ['dist/**', './require.js'],
	},
	{
		files: ['**/*.js'],
		languageOptions: {
			globals: {
				...globals.browser,
				define: 'readonly',
				require: 'readonly',
				requirejs: 'readonly',
			},
		},

		plugins: {
			js,
			requirejs: requirejsPlugin,
		},
		extends: ['js/recommended'],

		rules: {
			...requirejsPlugin.configs.recommended.rules,
			'no-unused-vars': [
				'error',
				{
					vars: 'all',
					args: 'none',
					argsIgnorePattern: '^_',
					caughtErrors: 'all',
					caughtErrorsIgnorePattern: '^_',
					destructuredArrayIgnorePattern: '^_',
					varsIgnorePattern: '^_',
					ignoreRestSiblings: true,
				},
			],
		},
	},
]);
