const settingsObject = {
	baseUrl: 'js',
	waitSeconds: 0, // this will be overridden in the compiled file
	config: {
		text: {
			useXhr: function (url, protocol, hostname, port) {
				return true;
			},
		},
	},
	packages: [
		{
			name: 'databindings',
			location: 'extensions/bindings',
		},
		{
			name: 'cohortdefinitionviewer',
			location: 'components/cohortdefinitionviewer',
		},
		{
			name: 'circe',
			location: 'components/circe',
		},
		{
			name: 'cyclops',
			location: 'components/cyclops',
		},
		{
			name: 'evidence',
			location: 'components/evidence',
		},
		{
			name: 'featureextraction',
			location: 'components/featureextraction',
		},
		{
			name: 'pages',
			location: 'pages',
		},
		{
			name: 'lodash',
			location: '../node_modules/lodash',
			main: 'lodash',
		},
		{
			name: 'urijs',
			location: '../node_modules/urijs/src',
			main: 'URI',
		},
		{
			name: 'utilities',
			location: 'components/utilities',
		},
	],
	shim: {
		colorbrewer: {
			exports: 'colorbrewer',
		},
		bootstrap: {
			deps: ['@popperjs/core'],
		},
		prism: {
			exports: 'Prism',
		},
		'prismlanguages/prism-sql': {
			deps: ['prism'],
		},
		xss: {
			exports: 'filterXSS',
		},
	},
	map: {
		'*': {
			'jqueryui/autoGrowInput': 'assets/jqueryui/autoGrowInput',
			'd3-color': 'd3',
			'd3-interpolate': 'd3',
			'd3-transition': 'd3',
			'services/VocabularyProvider': 'services/Vocabulary',
		},
	},
	paths: {
		'@popperjs/core': '../node_modules/@popperjs/core/dist/umd/popper',
		jquery: '../node_modules/jquery/dist/jquery',
		'jquery-ui': '../node_modules/jquery-ui',
		bootstrap: '../node_modules/bootstrap/dist/js/bootstrap',
		text: 'extensions/plugins/text',
		less: 'extensions/plugins/less',
		css: 'extensions/plugins/css',
		optional: 'extensions/plugins/optional',
		clipboard: '../node_modules/clipboard/dist/clipboard',
		knockout: '../node_modules/knockout/build/output/knockout-latest',
		'ko.sortable':
			'../node_modules/knockout-sortable/src/knockout-sortable',
		'knockout-mapping':
			'../node_modules/knockout-mapping/dist/knockout.mapping',
		'datatables.net': '../node_modules/datatables.net/js/dataTables',
		'datatables.net-bs5':
			'../node_modules/datatables.net-bs5/js/dataTables.bootstrap5',
		'datatables.net-buttons':
			'../node_modules/datatables.net-buttons/js/dataTables.buttons',
		'datatables.net-buttons-bs5':
			'../node_modules/datatables.net-buttons-bs5/js/buttons.bootstrap5',
		'datatables.net-buttons-html5':
			'../node_modules/datatables.net-buttons/js/buttons.html5',
		'datatables.net-select':
			'../node_modules/datatables.net-select/js/dataTables.select',
		'datatables.net-select-bs5':
			'../node_modules/datatables.net-select-bs5/js/select.bootstrap5',
		colvis: '../node_modules/datatables.net-buttons/js/buttons.colVis',
		crossfilter: '../node_modules/crossfilter2/crossfilter',
		director: '../node_modules/director/build/director',
		atlascharts:
			'../node_modules/@ohdsi/atlascharts/dist/atlascharts',
		jnj_chart: 'assets/jnj.chart', // scatterplot is not ported to separate library
		appConfig: 'config',
		prism: '../node_modules/prismjs/prism',
		prismlanguages: '../node_modules/prismjs/components',
		papaparse: '../node_modules/papaparse/papaparse',
		d3: '../node_modules/d3/dist/d3',
		'd3-collection': '../node_modules/d3-collection/dist/d3-collection',
		'd3-selection': '../node_modules/d3-selection/dist/d3-selection',
		'd3-tip': '../node_modules/d3-tip/dist/index',
		xss: '../node_modules/xss/dist/xss',

		moment: '../node_modules/moment/moment',
		querystring: '../node_modules/qs/dist/qs',

		'slim-select': '../node_modules/slim-select/dist/slimselect',
		'less-js': '../node_modules/less/dist/less',
		'file-saver': '../node_modules/file-saver/dist/FileSaver',
		svgsaver: '../node_modules/svgsaver/browser',
		jszip: '../node_modules/jszip/dist/jszip.min',
		numeral: '../node_modules/numeral/numeral',
		'lz-string': '../node_modules/lz-string/libs/lz-string',
		colorbrewer: '../node_modules/colorbrewer/index',
		flatpickr: '../node_modules/flatpickr/dist/flatpickr',
		visibilityjs: '../node_modules/@ohdsi/visibilityjs/lib/visibility.core',
		ajv: '../node_modules/ajv/dist/ajv.bundle',
		html2canvas: '../node_modules/html2canvas/dist/html2canvas.min',
		venn: '../node_modules/@upsetjs/venn.js/build/venn',
		bowser: '../node_modules/bowser/es5',
	},
	localRefs: {
		'atlas-state': 'components/atlas-state',
		configuration: 'components/configuration',
		'conceptset-editor': 'components/conceptset/conceptset-editor',
		'conceptset-modal': 'components/conceptsetmodal/conceptSetSaveModal',
		conceptpicker: 'components/conceptpicker',
		'faceted-datatable': 'components/faceted-datatable',
		feedback: 'components/feedback',
		forbidden: 'components/ac-forbidden',
		home: 'components/home',
		loading: 'components/loading',
		'r-manager': 'components/r-manager',
		'role-details': 'components/role-details',
		roles: 'components/roles',
		unauthenticated: 'components/ac-unauthenticated',
		'user-bar': 'components/userbar/user-bar',
		welcome: 'components/welcome',
	},
};

if (typeof define !== 'undefined') {
	define(() => settingsObject);
} else {
	module.exports = settingsObject;
}
